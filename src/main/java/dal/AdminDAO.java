package dal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Database operations used only by administrator tools. */
public class AdminDAO extends DBContext {
    public List<Map<String, Object>> exportUsers() {
        return query("""
                SELECT user_id,full_name,email,role,status,created_at
                FROM users WHERE COALESCE(status,'ACTIVE') <> 'DELETED'
                ORDER BY created_at DESC,user_id DESC""");
    }

    public List<Map<String, Object>> exportPatients() {
        return query("""
                SELECT p.patient_id,p.full_name,p.date_of_birth,p.gender,p.phone,
                       u.email,u.status
                FROM patients p LEFT JOIN users u ON u.user_id=p.user_id
                WHERE COALESCE(u.status,'ACTIVE') <> 'DELETED'
                ORDER BY p.created_at DESC,p.patient_id DESC""");
    }

    public List<Map<String, Object>> trashItems() {
        return query("""
                SELECT trash_id,entity_type,entity_id,display_name,details,
                       deleted_at,deleted_by,restored_at,purged_at
                FROM admin_trash
                WHERE restored_at IS NULL AND purged_at IS NULL
                ORDER BY deleted_at DESC,trash_id DESC""");
    }

    public List<Map<String, Object>> loginHistory(String eventType, String keyword, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT login_history_id,user_id,username,full_name,role,event_type,
                       ip_address,user_agent,session_id,occurred_at
                FROM login_history WHERE 1=1""");
        List<Object> parameters = new ArrayList<>();
        if (eventType != null && !eventType.isBlank()) {
            sql.append(" AND event_type=?");
            parameters.add(eventType);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (full_name ILIKE ? OR username ILIKE ? OR ip_address ILIKE ?)");
            String value = "%" + keyword.trim() + "%";
            parameters.add(value); parameters.add(value); parameters.add(value);
        }
        sql.append(" ORDER BY occurred_at DESC,login_history_id DESC LIMIT ?");
        parameters.add(Math.max(1, Math.min(limit, 500)));
        return query(sql.toString(), parameters.toArray());
    }

    public void softDeleteUser(int userId, int adminId) {
        inTransaction("Không thể đưa tài khoản vào thùng rác.", () -> {
            Map<String, Object> user = one("""
                    SELECT user_id,username,full_name,email,role,status
                    FROM users WHERE user_id=? FOR UPDATE""", userId);
            if (user == null) throw new IllegalArgumentException("Không tìm thấy tài khoản.");
            if (userId == adminId || "ADMIN".equals(user.get("role"))) {
                throw new IllegalArgumentException("Không thể xóa tài khoản quản trị viên.");
            }
            if ("DELETED".equals(user.get("status"))) {
                throw new IllegalArgumentException("Tài khoản đã ở trong thùng rác.");
            }
            String details = "Tên đăng nhập: " + value(user, "username")
                    + " | Email: " + value(user, "email")
                    + " | Vai trò: " + value(user, "role");
            update("""
                    INSERT INTO admin_trash(entity_type,entity_id,display_name,details,deleted_by)
                    VALUES('USER',?,?,?,?)""", userId, value(user, "full_name"), details, adminId);
            update("UPDATE users SET status='DELETED' WHERE user_id=?", userId);
            audit(adminId, "SOFT_DELETE", "USER", String.valueOf(userId), details);
        });
    }

    public void restore(long trashId, int adminId) {
        inTransaction("Không thể khôi phục dữ liệu.", () -> {
            Map<String, Object> item = one("""
                    SELECT trash_id,entity_type,entity_id FROM admin_trash
                    WHERE trash_id=? AND restored_at IS NULL AND purged_at IS NULL FOR UPDATE""",
                    trashId);
            if (item == null) throw new IllegalArgumentException("Mục thùng rác không tồn tại.");
            if (!"USER".equals(item.get("entity_type"))) {
                throw new IllegalArgumentException("Loại dữ liệu này chưa hỗ trợ khôi phục.");
            }
            int entityId = ((Number) item.get("entity_id")).intValue();
            if (update("UPDATE users SET status='ACTIVE' WHERE user_id=? AND status='DELETED'",
                    entityId) != 1) {
                throw new IllegalArgumentException("Tài khoản không còn tồn tại để khôi phục.");
            }
            update("UPDATE admin_trash SET restored_by=?,restored_at=CURRENT_TIMESTAMP WHERE trash_id=?",
                    adminId, trashId);
            audit(adminId, "RESTORE", "USER", String.valueOf(entityId), "Khôi phục từ thùng rác");
        });
    }

    public void purge(long trashId, int adminId) {
        inTransaction("Không thể xóa vĩnh viễn dữ liệu.", () -> {
            Map<String, Object> item = one("""
                    SELECT trash_id,entity_type,entity_id FROM admin_trash
                    WHERE trash_id=? AND restored_at IS NULL AND purged_at IS NULL FOR UPDATE""",
                    trashId);
            if (item == null) throw new IllegalArgumentException("Mục thùng rác không tồn tại.");
            if (!"USER".equals(item.get("entity_type"))) {
                throw new IllegalArgumentException("Loại dữ liệu này chưa hỗ trợ xóa vĩnh viễn.");
            }
            int entityId = ((Number) item.get("entity_id")).intValue();
            audit(adminId, "PURGE", "USER", String.valueOf(entityId),
                    "Xóa vĩnh viễn từ thùng rác");
            try {
                if (update("DELETE FROM users WHERE user_id=? AND status='DELETED'", entityId) != 1) {
                    throw new IllegalArgumentException("Tài khoản không còn tồn tại.");
                }
            } catch (SQLException error) {
                if ("23503".equals(error.getSQLState())) {
                    throw new IllegalArgumentException(
                            "Không thể xóa vĩnh viễn vì tài khoản đang có dữ liệu liên quan.");
                }
                throw error;
            }
            update("UPDATE admin_trash SET purged_by=?,purged_at=CURRENT_TIMESTAMP WHERE trash_id=?",
                    adminId, trashId);
        });
    }

    private List<Map<String, Object>> query(String sql, Object... parameters) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                ResultSetMetaData metadata = result.getMetaData();
                while (result.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= metadata.getColumnCount(); i++) {
                        row.put(metadata.getColumnLabel(i), result.getObject(i));
                    }
                    rows.add(row);
                }
            }
        } catch (SQLException error) {
            throw databaseError("query administrator data", error);
        }
        return rows;
    }

    private Map<String, Object> one(String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                ResultSetMetaData metadata = result.getMetaData();
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    row.put(metadata.getColumnLabel(i), result.getObject(i));
                }
                return row;
            }
        }
    }

    private int update(String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            return statement.executeUpdate();
        }
    }

    private void inTransaction(String message, TransactionWork work) {
        try {
            connection.setAutoCommit(false);
            work.run();
            connection.commit();
        } catch (Exception error) {
            try { connection.rollback(); } catch (SQLException ignored) { }
            if (error instanceof IllegalArgumentException validation) throw validation;
            throw new IllegalStateException(message, error);
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) { }
        }
    }

    @FunctionalInterface
    private interface TransactionWork { void run() throws Exception; }

    private void audit(int actor, String action, String type, String entityId, String details)
            throws SQLException {
        update("INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details) VALUES(?,?,?,?,?)",
                actor, action, type, entityId, details);
    }

    private String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private void bind(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]);
    }
}
