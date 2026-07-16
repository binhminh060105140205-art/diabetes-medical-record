package dal;

import models.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import vn.diabetes.auth.Passwords;

public class UserDAO extends DBContext {
    PreparedStatement stm;
    ResultSet rs;

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setFullName(rs.getString("full_name"));
        u.setPhone(rs.getString("phone"));
        String role = rs.getString("role");
        u.setRole(role != null ? role.toUpperCase() : null);
        u.setStatus(rs.getString("status"));
        u.setEmail(rs.getString("email"));
        
        if (rs.getDate("dob") != null) {
            u.setDob(rs.getDate("dob"));
        }
        u.setGender(rs.getString("gender"));
        u.setAddress(rs.getString("address"));
        
        u.setCccd(rs.getString("cccd"));
        
        return u;
    }

    public User getById(int id) {
        String sql = "SELECT * FROM Users WHERE user_id=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, id);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw databaseError("load user", e);
        }
        return null;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM Users WHERE username=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setString(1, username);
            rs = stm.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw databaseError("check username", e);
        }
    }

    /** Loads counters, filtered total and the requested user page in one database round-trip. */
    public AdminDashboardData loadAdminDashboard(String role, String keyword, int page, int pageSize) {
        boolean hasRole = role != null && !role.isBlank();
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (hasRole) where.append(" AND role=?");
        if (hasKeyword) where.append(" AND (full_name ILIKE ? OR username ILIKE ? OR phone ILIKE ?)");
        String sql = """
            WITH metrics AS (
              SELECT (SELECT COUNT(*) FROM patients) patients,
                     COUNT(*) FILTER (WHERE role='DOCTOR') doctors,
                     COUNT(*) FILTER (WHERE role='STAFF') staff
              FROM users
            ), filtered AS (SELECT * FROM users""" + where + "), total AS (SELECT COUNT(*) total FROM filtered), "
                + "page_data AS (SELECT * FROM filtered ORDER BY created_at DESC LIMIT ? OFFSET ?) "
                + "SELECT p.*,m.patients,m.doctors,m.staff,t.total FROM metrics m CROSS JOIN total t LEFT JOIN page_data p ON TRUE";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            if (hasRole) ps.setString(index++, role);
            if (hasKeyword) {
                String value = "%" + keyword.trim() + "%";
                ps.setString(index++, value); ps.setString(index++, value); ps.setString(index++, value);
            }
            ps.setInt(index++, pageSize); ps.setInt(index, (page - 1) * pageSize);
            try (ResultSet rows = ps.executeQuery()) {
                int total = 0, patients = 0, doctors = 0, staff = 0;
                while (rows.next()) {
                    total = rows.getInt("total"); patients = rows.getInt("patients");
                    doctors = rows.getInt("doctors"); staff = rows.getInt("staff");
                    if (rows.getObject("user_id") != null) users.add(mapRow(rows));
                }
                return new AdminDashboardData(users, total, patients, doctors, staff);
            }
        } catch (SQLException error) { throw databaseError("load admin dashboard", error); }
    }

    public record AdminDashboardData(List<User> users, int filteredTotal,
            int patients, int doctors, int staff) {}

    public User create(User u) {
        String sql = "INSERT INTO Users(username, password, full_name, phone, role, status, email, dob, gender, address, cccd) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try {
            stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stm.setString(1, u.getUsername());
            stm.setString(2, Passwords.encode(u.getPassword()));
            stm.setString(3, u.getFullName());
            stm.setString(4, u.getPhone());
            stm.setString(5, u.getRole());
            stm.setString(6, "ACTIVE");
            stm.setString(7, u.getEmail());
            
            if (u.getDob() != null) {
                stm.setDate(8, new java.sql.Date(u.getDob().getTime()));
            } else {
                stm.setNull(8, java.sql.Types.DATE);
            }
            
            stm.setString(9, u.getGender());
            stm.setString(10, u.getAddress());
            stm.setString(11, u.getCccd());
            
            int rows = stm.executeUpdate();
            if (rows > 0) {
                rs = stm.getGeneratedKeys();
                if (rs.next()) {
                    u.setUserId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw databaseError("create user", e);
        }
        return u;
    }

    public void update(User u) {
        String sql = "UPDATE Users SET full_name=?,phone=?,role=?,status=? WHERE user_id=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setString(1, u.getFullName());
            stm.setString(2, u.getPhone());
            stm.setString(3, u.getRole());
            stm.setString(4, u.getStatus());
            stm.setInt(5, u.getUserId());
            stm.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("update user", e);
        }
    }

    public void updateProfile(User u) throws SQLException {
        String sql = "UPDATE Users SET username=?, password=?, full_name=?, phone=?, email=?, dob=?, gender=?, cccd=? WHERE user_id=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setString(1, u.getUsername());
            stm.setString(2, Passwords.isEncoded(u.getPassword()) ? u.getPassword() : Passwords.encode(u.getPassword()));
            stm.setString(3, u.getFullName());
            stm.setString(4, u.getPhone());
            stm.setString(5, u.getEmail());
            if (u.getDob() != null) {
                stm.setDate(6, new java.sql.Date(u.getDob().getTime()));
            } else {
                stm.setNull(6, java.sql.Types.DATE);
            }
            stm.setString(7, u.getGender());
            stm.setString(8, u.getCccd());
            stm.setInt(9, u.getUserId());
            stm.executeUpdate();
        } catch (SQLException e) {
            throw e;
        }
    }

    public List<User> getAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM Users ORDER BY created_at DESC";
        try {
            stm = connection.prepareStatement(sql);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw databaseError("load users", e);
        }
        return list;
    }

}
