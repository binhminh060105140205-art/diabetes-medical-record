package vn.diabetes.auth;

import java.sql.Timestamp;
import java.time.Instant;
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

    public LoginSecurityState getLoginSecurityState(int userId) {
        return jdbc.sql("""
                SELECT COALESCE(failed_login_attempts, 0) AS failed_login_attempts, lock_until
                FROM users
                WHERE user_id = :id
                """)
                .param("id", userId)
                .query((rs, rowNum) -> mapLoginSecurityState(
                        rs.getInt("failed_login_attempts"), rs.getTimestamp("lock_until")))
                .optional()
                .orElse(LoginSecurityState.clear());
    }

    public LoginSecurityState recordFailedLogin(int userId, int maxAttempts, Instant lockUntil) {
        return jdbc.sql("""
                UPDATE users
                SET failed_login_attempts = COALESCE(failed_login_attempts, 0) + 1,
                    lock_until = CASE
                        WHEN COALESCE(failed_login_attempts, 0) + 1 >= :maxAttempts
                        THEN :lockUntil
                        ELSE NULL
                    END
                WHERE user_id = :id
                RETURNING failed_login_attempts, lock_until
                """)
                .param("id", userId)
                .param("maxAttempts", maxAttempts)
                .param("lockUntil", Timestamp.from(lockUntil))
                .query((rs, rowNum) -> mapLoginSecurityState(
                        rs.getInt("failed_login_attempts"), rs.getTimestamp("lock_until")))
                .optional()
                .orElse(LoginSecurityState.clear());
    }

    public void clearLoginFailures(int userId) {
        jdbc.sql("""
                UPDATE users
                SET failed_login_attempts = 0, lock_until = NULL
                WHERE user_id = :id
                """)
                .param("id", userId)
                .update();
    }

    private LoginSecurityState mapLoginSecurityState(int failedAttempts, Timestamp lockUntil) {
        return new LoginSecurityState(failedAttempts, lockUntil == null ? null : lockUntil.toInstant());
    }

    public record LoginSecurityState(int failedAttempts, Instant lockUntil) {
        static LoginSecurityState clear() {
            return new LoginSecurityState(0, null);
        }
    }
}
