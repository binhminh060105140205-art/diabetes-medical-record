package dal;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Writes the small security audit trail shown to administrators. */
public class LoginHistoryDAO extends DBContext {
    public void record(Integer userId, String username, String fullName, String role,
            String eventType, String ipAddress, String userAgent, String sessionId) {
        String sql = """
                INSERT INTO login_history(
                    user_id,username,full_name,role,event_type,ip_address,user_agent,session_id)
                VALUES(?,?,?,?,?,?,?,?)""";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId == null) statement.setNull(1, java.sql.Types.INTEGER);
            else statement.setInt(1, userId);
            statement.setString(2, username);
            statement.setString(3, fullName);
            statement.setString(4, role);
            statement.setString(5, eventType);
            statement.setString(6, limit(ipAddress, 100));
            statement.setString(7, limit(userAgent, 500));
            statement.setString(8, limit(sessionId, 120));
            statement.executeUpdate();
        } catch (SQLException error) {
            throw databaseError("record login history", error);
        }
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
