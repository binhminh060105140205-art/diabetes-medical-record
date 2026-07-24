package dal;

import models.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import vn.diabetes.auth.Passwords;

public class UserDAO extends DBContext {
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
        u.setCreatedAt(rs.getTimestamp("created_at"));
        
        return u;
    }

    public User getById(int id) {
        String sql = "SELECT * FROM Users WHERE user_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? mapRow(rows) : null;
            }
        } catch (SQLException e) {
            throw databaseError("load user", e);
        }
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM Users WHERE username=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException e) {
            throw databaseError("check username", e);
        }
    }

    /** Loads counters, filtered total and the requested user page in one database round-trip. */
    public AdminDashboardData loadAdminDashboard(String role, String status, String keyword,
            String sortOrder, int page, int pageSize) {
        boolean hasRole = role != null && !role.isBlank();
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        // Soft-deleted accounts are managed in AdminTrash and stay out of the normal list.
        StringBuilder where = new StringBuilder(" WHERE COALESCE(status,'ACTIVE') <> 'DELETED'");
        if (hasRole) where.append(" AND role=?");
        if (hasStatus) where.append(" AND status=?");
        if (hasKeyword) {
            where.append(" AND (").append(SearchSupport.contains("full_name"))
                    .append(" OR LOWER(username) LIKE LOWER(?) OR LOWER(phone) LIKE LOWER(?))");
        }
        String ordering = "OLDEST".equals(sortOrder)
                ? "created_at ASC, user_id ASC" : "created_at DESC, user_id DESC";
        String sql = """
            WITH metrics AS (
              SELECT (SELECT COUNT(*) FROM patients p LEFT JOIN users pu ON pu.user_id=p.user_id
                              WHERE COALESCE(pu.status,'ACTIVE') <> 'DELETED') patients,
                     COUNT(*) total_users,
                     SUM(CASE WHEN role='DOCTOR' THEN 1 ELSE 0 END) doctors,
                     SUM(CASE WHEN role='STAFF' THEN 1 ELSE 0 END) staff
              FROM users
              WHERE COALESCE(status,'ACTIVE') <> 'DELETED'
            ), filtered AS (SELECT * FROM users""" + where + "), total AS (SELECT COUNT(*) total FROM filtered), "
                + "page_data AS (SELECT * FROM filtered ORDER BY " + ordering + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY) "
                + "SELECT p.*,m.total_users,m.patients,m.doctors,m.staff,t.total FROM metrics m CROSS JOIN total t LEFT JOIN page_data p ON 1=1";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            if (hasRole) ps.setString(index++, role);
            if (hasStatus) ps.setString(index++, status);
            if (hasKeyword) {
                String value = "%" + keyword.trim() + "%";
                ps.setString(index++, value); ps.setString(index++, value); ps.setString(index++, value);
            }
            ps.setInt(index++, (page - 1) * pageSize); ps.setInt(index, pageSize);
            try (ResultSet rows = ps.executeQuery()) {
                int total = 0, totalUsers = 0, patients = 0, doctors = 0, staff = 0;
                while (rows.next()) {
                    total = rows.getInt("total"); patients = rows.getInt("patients");
                    totalUsers = rows.getInt("total_users");
                    doctors = rows.getInt("doctors"); staff = rows.getInt("staff");
                    if (rows.getObject("user_id") != null) users.add(mapRow(rows));
                }
                return new AdminDashboardData(users, total, totalUsers, patients, doctors, staff);
            }
        } catch (SQLException error) { throw databaseError("load admin dashboard", error); }
    }

    public record AdminDashboardData(List<User> users, int filteredTotal, int totalUsers,
            int patients, int doctors, int staff) {}

    public User create(User u) {
        String sql = "INSERT INTO Users(username, password, full_name, phone, role, status, email, dob, gender, address, cccd) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, u.getUsername());
            statement.setString(2, Passwords.encode(u.getPassword()));
            statement.setString(3, u.getFullName());
            statement.setString(4, u.getPhone());
            statement.setString(5, u.getRole());
            statement.setString(6, "ACTIVE");
            statement.setString(7, u.getEmail());
            
            if (u.getDob() != null) {
                statement.setDate(8, new java.sql.Date(u.getDob().getTime()));
            } else {
                statement.setNull(8, java.sql.Types.DATE);
            }
            
            statement.setString(9, u.getGender());
            statement.setString(10, u.getAddress());
            statement.setString(11, u.getCccd());
            
            if (statement.executeUpdate() > 0) {
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) u.setUserId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw databaseError("create user", e);
        }
        return u;
    }

    public void update(User u) {
        String sql = "UPDATE Users SET full_name=?,phone=?,role=?,status=? WHERE user_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, u.getFullName());
            statement.setString(2, u.getPhone());
            statement.setString(3, u.getRole());
            statement.setString(4, u.getStatus());
            statement.setInt(5, u.getUserId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("update user", e);
        }
    }

    public void updateProfile(User u) throws SQLException {
        String sql = "UPDATE Users SET username=?, password=?, full_name=?, phone=?, email=?, dob=?, gender=?, address=?, cccd=? WHERE user_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, u.getUsername());
            statement.setString(2, Passwords.isEncoded(u.getPassword())
                    ? u.getPassword() : Passwords.encode(u.getPassword()));
            statement.setString(3, u.getFullName());
            statement.setString(4, u.getPhone());
            statement.setString(5, u.getEmail());
            if (u.getDob() != null) {
                statement.setDate(6, new java.sql.Date(u.getDob().getTime()));
            } else {
                statement.setNull(6, java.sql.Types.DATE);
            }
            statement.setString(7, u.getGender());
            statement.setString(8, u.getAddress());
            statement.setString(9, u.getCccd());
            statement.setInt(10, u.getUserId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw e;
        }
    }

}
