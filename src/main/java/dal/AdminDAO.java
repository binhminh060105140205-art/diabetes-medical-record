package dal;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import models.Doctor;
import models.User;
import vn.diabetes.auth.Passwords;

/** Database operations used only by administrator tools. */
public class AdminDAO extends DBContext {
    /** Creates the user and doctor profile atomically so a failed doctor insert leaves no orphan account. */
    public CreatedAccount createManagedAccount(User user, Doctor doctor, int adminId) {
        boolean originalAutoCommit;
        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException error) {
            throw databaseError("start account creation", error);
        }

        try {
            String userSql = """
                    INSERT INTO users(username,password,full_name,phone,role,status,email,dob,gender,address,cccd)
                    VALUES(?,?,?,?,?,'ACTIVE',?,?,?,?,?)
                    """;
            try (PreparedStatement statement = connection.prepareStatement(
                    userSql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, user.getUsername());
                statement.setString(2, Passwords.encode(user.getPassword()));
                statement.setString(3, user.getFullName());
                statement.setString(4, user.getPhone());
                statement.setString(5, user.getRole());
                statement.setString(6, user.getEmail());
                statement.setDate(7, user.getDob() == null
                        ? null : new Date(user.getDob().getTime()));
                statement.setString(8, user.getGender());
                statement.setString(9, user.getAddress());
                statement.setString(10, user.getCccd());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("User ID was not returned");
                    user.setUserId(keys.getInt(1));
                }
            }

            if (doctor != null) {
                doctor.setUserId(user.getUserId());
                String doctorSql = """
                        INSERT INTO doctors(user_id,specialty,license_no,degree,diabetes_focus)
                        VALUES(?,?,?,?,?)
                        """;
                try (PreparedStatement statement = connection.prepareStatement(
                        doctorSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, doctor.getUserId());
                    statement.setString(2, Doctor.DIABETES_SPECIALTY);
                    statement.setString(3, doctor.getLicenseNo());
                    statement.setString(4, doctor.getDegree());
                    statement.setString(5, doctor.getDiabetesFocus());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Doctor ID was not returned");
                        doctor.setDoctorId(keys.getInt(1));
                    }
                }
            }

            audit(adminId, "CREATE", doctor == null ? "STAFF_ACCOUNT" : "DOCTOR_ACCOUNT",
                    String.valueOf(user.getUserId()), user.getUsername());
            connection.commit();
            return new CreatedAccount(user, doctor);
        } catch (SQLException error) {
            try { connection.rollback(); } catch (SQLException ignored) { }
            if ("23505".equals(error.getSQLState())) {
                throw new IllegalArgumentException(duplicateAccountMessage(error), error);
            }
            throw databaseError("create managed account", error);
        } finally {
            try { connection.setAutoCommit(originalAutoCommit); } catch (SQLException ignored) { }
        }
    }

    public record CreatedAccount(User user, Doctor doctor) {}

    private String duplicateAccountMessage(SQLException error) {
        String detail = String.valueOf(error.getMessage()).toLowerCase(Locale.ROOT);
        if (detail.contains("username")) return "Tên đăng nhập đã tồn tại.";
        if (detail.contains("license_no")) return "Số chứng chỉ hành nghề đã tồn tại.";
        if (detail.contains("cccd")) return "Số CCCD đã được sử dụng.";
        return "Thông tin tài khoản bị trùng với dữ liệu hiện có.";
    }

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
            ensureUserCanBeDeleted(user);
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

    /** Safe account deletion entry point used by the Admin dashboard. */
    public void deleteUser(int userId, int adminId) {
        softDeleteUser(userId, adminId);
    }

    /** Changes availability without disabling an account that still owns active work. */
    public String toggleUserStatus(int userId, int adminId) {
        final String[] result = new String[1];
        inTransaction("Không thể cập nhật trạng thái tài khoản.", () -> {
            Map<String, Object> user = one("""
                    SELECT user_id,username,full_name,role,status
                    FROM users WHERE user_id=? FOR UPDATE""", userId);
            if (user == null) throw new IllegalArgumentException("Không tìm thấy tài khoản.");
            if (userId == adminId || "ADMIN".equals(user.get("role"))) {
                throw new IllegalArgumentException("Không thể khóa tài khoản quản trị viên.");
            }
            String currentStatus = String.valueOf(user.get("status"));
            if ("DELETED".equals(currentStatus)) {
                throw new IllegalArgumentException("Tài khoản đã ở trong thùng rác.");
            }
            String nextStatus = "ACTIVE".equals(currentStatus) ? "INACTIVE" : "ACTIVE";
            if ("INACTIVE".equals(nextStatus)) {
                try {
                    ensureUserCanBeDeleted(user);
                } catch (IllegalArgumentException error) {
                    String message = error.getMessage();
                    if (message != null) {
                        message = message.replace("Không thể xóa tài khoản này vì",
                                "Không thể khóa tài khoản này vì");
                    }
                    throw new IllegalArgumentException(message, error);
                }
            }
            update("UPDATE users SET status=? WHERE user_id=?", nextStatus, userId);
            audit(adminId, "ACTIVE".equals(nextStatus) ? "ACTIVATE" : "DEACTIVATE",
                    "USER", String.valueOf(userId), value(user, "username"));
            result[0] = nextStatus;
        });
        return result[0];
    }

    /** Prevents removing an account that still owns active clinical work. */
    private void ensureUserCanBeDeleted(Map<String, Object> user) throws SQLException {
        int userId = ((Number) user.get("user_id")).intValue();
        String role = value(user, "role");
        List<String> blockers = new ArrayList<>();
        if ("PATIENT".equalsIgnoreCase(role)) {
            if (count("""
                    SELECT COUNT(*) FROM appointments a
                    JOIN patients p ON p.patient_id=a.patient_id
                    WHERE p.user_id=? AND a.status IN ('REQUESTED','BOOKED','CONFIRMED','CHECKED_IN')""", userId) > 0) {
                blockers.add("bệnh nhân còn lịch hẹn đang chờ hoặc đang khám");
            }
            if (count("""
                    SELECT COUNT(*) FROM encounters e
                    JOIN patients p ON p.patient_id=e.patient_id
                    WHERE p.user_id=? AND e.status NOT IN ('COMPLETED','CANCELLED')""", userId) > 0) {
                blockers.add("bệnh nhân đang ở hàng đợi hoặc trong lượt khám");
            }
            if (count("""
                    SELECT COUNT(*) FROM medicalrecords r
                    JOIN patients p ON p.patient_id=r.patient_id
                    WHERE p.user_id=? AND r.status='DRAFT'""", userId) > 0) {
                blockers.add("bệnh án của bệnh nhân chưa được bác sĩ hoàn tất");
            }
        } else if ("DOCTOR".equalsIgnoreCase(role)) {
            if (count("""
                    SELECT COUNT(*) FROM appointments a
                    JOIN doctors d ON d.doctor_id=a.doctor_id
                    WHERE d.user_id=? AND a.status IN ('BOOKED','CONFIRMED','CHECKED_IN')""", userId) > 0) {
                blockers.add("bác sĩ còn lịch hẹn đã phân công");
            }
            if (count("""
                    SELECT COUNT(*) FROM encounters e
                    JOIN doctors d ON d.doctor_id=e.doctor_id
                    WHERE d.user_id=? AND e.status NOT IN ('COMPLETED','CANCELLED')""", userId) > 0) {
                blockers.add("bác sĩ đang có lượt khám chưa hoàn tất");
            }
            if (count("""
                    SELECT COUNT(*) FROM medicalrecords r
                    JOIN doctors d ON d.doctor_id=r.doctor_id
                    WHERE d.user_id=? AND r.status='DRAFT'""", userId) > 0) {
                blockers.add("bác sĩ còn bệnh án đang xử lý");
            }
            if (count("""
                    SELECT COUNT(*) FROM lab_orders l
                    JOIN doctors d ON d.doctor_id=l.doctor_id
                    WHERE d.user_id=? AND l.status IN ('ORDERED','COLLECTED','RESULTED')""", userId) > 0) {
                blockers.add("bác sĩ còn phiếu xét nghiệm chưa hoàn tất");
            }
        } else if ("STAFF".equalsIgnoreCase(role)) {
            if (count("""
                    SELECT COUNT(*) FROM encounters
                    WHERE created_by=? AND status NOT IN ('COMPLETED','CANCELLED')""", userId) > 0) {
                blockers.add("nhân viên còn lượt tiếp nhận đang xử lý");
            }
            if (count("""
                    SELECT COUNT(*) FROM medicalrecords
                    WHERE created_by_staff=? AND status='DRAFT'""", userId) > 0) {
                blockers.add("nhân viên còn bệnh án nhập dở");
            }
            if (count("""
                    SELECT COUNT(*) FROM lab_orders
                    WHERE resulted_by=? AND status='RESULTED'""", userId) > 0) {
                blockers.add("nhân viên còn kết quả xét nghiệm chờ bác sĩ xác nhận");
            }
            if (count("""
                    SELECT COUNT(*) FROM appointments
                    WHERE created_by=? AND status IN ('REQUESTED','BOOKED','CONFIRMED','CHECKED_IN')""", userId) > 0) {
                blockers.add("nhân viên còn lịch hẹn đang điều phối");
            }
        }
        if (!blockers.isEmpty()) {
            throw new IllegalArgumentException("Không thể xóa tài khoản này vì "
                    + String.join("; ", blockers)
                    + ". Hãy hoàn tất hoặc hủy các công việc trước.");
        }
    }

    private long count(String sql, Object... parameters) throws SQLException {
        Map<String, Object> row = one(sql, parameters);
        if (row == null || row.get("count") == null) return 0;
        return ((Number) row.get("count")).longValue();
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
