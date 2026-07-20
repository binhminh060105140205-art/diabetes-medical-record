package dal;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import vn.diabetes.auth.Passwords;

/** Creates the portal account and patient profile atomically. */
public class PatientRegistrationDAO extends DBContext implements vn.diabetes.service.PatientRegistrationGateway {
    public int register(String username, String password, String fullName, String phone,
                        String email, LocalDate dob, String gender, String address,
                        String insuranceNo, Integer createdBy) {
        try {
            connection.setAutoCommit(false);
            if (exists("SELECT 1 FROM users WHERE LOWER(username)=LOWER(?)", username))
                throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng.");
            if (exists("SELECT 1 FROM patients WHERE phone=?", phone))
                throw new IllegalArgumentException("Số điện thoại đã có hồ sơ. Vui lòng đăng nhập hoặc liên hệ lễ tân.");
            if (email != null && !email.isBlank()
                    && exists("SELECT 1 FROM users WHERE LOWER(email)=LOWER(?)", email))
                throw new IllegalArgumentException("Thư điện tử đã được sử dụng.");

            int userId;
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO users(username,password,full_name,phone,role,status,email,dob,gender,address)
                    VALUES(?,?,?,?, 'PATIENT','ACTIVE',?,?,?,?)""", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, Passwords.encode(password));
                ps.setString(3, fullName);
                ps.setString(4, phone);
                nullable(ps, 5, email);
                ps.setDate(6, dob == null ? null : Date.valueOf(dob));
                ps.setString(7, gender);
                nullable(ps, 8, address);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); userId = rs.getInt(1); }
            }
            int patientId;
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO patients(user_id,full_name,date_of_birth,gender,phone,address,health_insurance_no,created_by)
                    VALUES(?,?,?,?,?,?,?,?)""", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId); ps.setString(2, fullName);
                ps.setDate(3, dob == null ? null : Date.valueOf(dob)); ps.setString(4, gender);
                ps.setString(5, phone); nullable(ps, 6, address); nullable(ps, 7, insuranceNo);
                if (createdBy == null) ps.setNull(8, Types.INTEGER); else ps.setInt(8, createdBy);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); patientId = rs.getInt(1); }
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO diabetes_profiles(patient_id) VALUES(?) ON CONFLICT (patient_id) DO NOTHING")) {
                ps.setInt(1, patientId);
                ps.executeUpdate();
            }
            connection.commit();
            return patientId;
        } catch (IllegalArgumentException ex) {
            rollback(); throw ex;
        } catch (SQLException ex) {
            rollback(); throw databaseError("register patient", ex);
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    private boolean exists(String sql, String value) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
    private void nullable(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) ps.setNull(index, Types.VARCHAR); else ps.setString(index, value.trim());
    }
    private void rollback() { try { connection.rollback(); } catch (SQLException ignored) {} }
}
