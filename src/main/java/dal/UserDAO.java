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
        
        try {
            u.setCccd(rs.getString("cccd"));
        } catch (SQLException ignored) {
        }
        
        return u;
    }

    public User login(String username, String password) {
        String sql = "SELECT * FROM Users WHERE username=? AND password=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setString(1, username);
            stm.setString(2, password);
            rs = stm.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            System.out.println("UserDAO.login ERROR: " + e.getMessage());
        }
        return null;
    }

    public User getById(int id) {
        String sql = "SELECT * FROM Users WHERE user_id=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, id);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.out.println("UserDAO.getById: " + e.getMessage());
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
            System.out.println("UserDAO.usernameExists: " + e.getMessage());
        }
        return false;
    }

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
            System.out.println("UserDAO.create ERROR: " + e.getMessage());
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
            System.out.println("UserDAO.update: " + e.getMessage());
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
            System.out.println("UserDAO.updateProfile: " + e.getMessage());
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
            System.out.println("UserDAO.getAll: " + e.getMessage());
        }
        return list;
    }

    public List<User> getByRole(String role) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM Users WHERE role=? ORDER BY full_name";
        try {
            stm = connection.prepareStatement(sql);
            stm.setString(1, role);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("UserDAO.getByRole: " + e.getMessage());
        }
        return list;
    }

    public int countWithFilter(String role) {
        String sql = (role == null || role.isEmpty()) ? "SELECT COUNT(*) FROM Users" : "SELECT COUNT(*) FROM Users WHERE role=?";
        try {
            stm = connection.prepareStatement(sql);
            if (role != null && !role.isEmpty()) {
                stm.setString(1, role);
            }
            rs = stm.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("UserDAO.countWithFilter: " + e.getMessage());
        }
        return 0;
    }

    public List<User> getWithPagingAndFilter(String role, int pageIndex, int pageSize) {
        List<User> list = new ArrayList<>();
        String sql = (role == null || role.isEmpty()) 
            ? "SELECT * FROM Users ORDER BY created_at DESC LIMIT ? OFFSET ?"
            : "SELECT * FROM Users WHERE role=? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try {
            stm = connection.prepareStatement(sql);
            if (role == null || role.isEmpty()) {
                stm.setInt(1, pageSize);
                stm.setInt(2, (pageIndex - 1) * pageSize);
            } else {
                stm.setString(1, role);
                stm.setInt(2, pageSize);
                stm.setInt(3, (pageIndex - 1) * pageSize);
            }
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("UserDAO.getWithPagingAndFilter: " + e.getMessage());
        }
        return list;
    }

    public int countWithFilter(String role, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Users WHERE 1=1");
        if (role != null && !role.isEmpty())    sql.append(" AND role=?");
        if (keyword != null && !keyword.isEmpty()) sql.append(" AND (full_name ILIKE ? OR username ILIKE ? OR phone ILIKE ?)");
        try {
            stm = connection.prepareStatement(sql.toString());
            int idx = 1;
            if (role != null && !role.isEmpty())        stm.setString(idx++, role);
            if (keyword != null && !keyword.isEmpty()) {
                String kw = "%" + keyword + "%";
                stm.setString(idx++, kw); stm.setString(idx++, kw); stm.setString(idx++, kw);
            }
            rs = stm.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.out.println("UserDAO.countWithFilter2: " + e.getMessage()); }
        return 0;
    }

    public List<User> getWithPagingAndFilter(String role, String keyword, int pageIndex, int pageSize) {
        List<User> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM Users WHERE 1=1");
        if (role != null && !role.isEmpty())       sql.append(" AND role=?");
        if (keyword != null && !keyword.isEmpty()) sql.append(" AND (full_name ILIKE ? OR username ILIKE ? OR phone ILIKE ?)");
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        try {
            stm = connection.prepareStatement(sql.toString());
            int idx = 1;
            if (role != null && !role.isEmpty())        stm.setString(idx++, role);
            if (keyword != null && !keyword.isEmpty()) {
                String kw = "%" + keyword + "%";
                stm.setString(idx++, kw); stm.setString(idx++, kw); stm.setString(idx++, kw);
            }
            stm.setInt(idx++, pageSize);
            stm.setInt(idx,   (pageIndex - 1) * pageSize);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("UserDAO.getWithPaging2: " + e.getMessage()); }
        return list;
    }
}
