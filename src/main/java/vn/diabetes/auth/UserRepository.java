package vn.diabetes.auth;

import java.util.Optional;
import models.User;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findActiveByUsername(String username) {
        return jdbc.sql("""
                SELECT user_id, username, password, full_name, phone, role, status,
                       email, dob, gender, address, cccd
                FROM Users
                WHERE username = :username
                  AND (status IS NULL OR status = 'ACTIVE')
                """)
                .param("username", username)
                .query((rs, rowNum) -> {
                    User user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setFullName(rs.getString("full_name"));
                    user.setPhone(rs.getString("phone"));
                    String role = rs.getString("role");
                    user.setRole(role == null ? null : role.toUpperCase());
                    user.setStatus(rs.getString("status"));
                    user.setEmail(rs.getString("email"));
                    user.setDob(rs.getDate("dob"));
                    user.setGender(rs.getString("gender"));
                    user.setAddress(rs.getString("address"));
                    user.setCccd(rs.getString("cccd"));
                    return user;
                }).optional();
    }

    public void updatePassword(int userId, String encodedPassword) {
        jdbc.sql("UPDATE users SET password=:password WHERE user_id=:id")
                .param("password", encodedPassword).param("id", userId).update();
    }
}
