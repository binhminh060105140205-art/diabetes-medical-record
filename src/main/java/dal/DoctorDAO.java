package dal;

import models.Doctor;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDAO extends DBContext {

    private Doctor mapRow(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setDoctorId(rs.getInt("doctor_id"));
        d.setUserId(rs.getInt("user_id"));
        d.setSpecialty(rs.getString("specialty"));
        d.setLicenseNo(rs.getString("license_no"));
        d.setFaceImagePath(rs.getString("face_image_path"));
        d.setCccdImagePath(rs.getString("cccd_image_path"));
        d.setCccdBackImagePath(rs.getString("cccd_back_image_path"));
        d.setLicenseImagePath(rs.getString("license_image_path"));
        Date issueDate = rs.getDate("license_issue_date");
        Date expireDate = rs.getDate("license_expire_date");
        d.setLicenseIssueDate(issueDate == null ? null : issueDate.toLocalDate());
        d.setLicenseExpireDate(expireDate == null ? null : expireDate.toLocalDate());
        d.setLicenseIssuedBy(rs.getString("license_issued_by"));
        d.setDegree(rs.getString("degree"));
        d.setConsultationFee(rs.getBigDecimal("consultation_fee"));
        try { d.setDiabetesFocus(rs.getString("diabetes_focus")); } catch (SQLException ignored) {}
        try { d.setFullName(rs.getString("full_name")); } catch (Exception ignored) {}
        return d;
    }

    public Doctor getByUserId(int userId) {
        String sql = "SELECT d.*, u.full_name FROM Doctors d JOIN Users u ON d.user_id=u.user_id WHERE d.user_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? mapRow(rows) : null;
            }
        } catch (SQLException e) {
            throw databaseError("load doctor account", e);
        }
    }

    public Doctor getById(int doctorId) {
        String sql = "SELECT d.*, u.full_name FROM Doctors d JOIN Users u ON d.user_id=u.user_id WHERE d.doctor_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, doctorId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? mapRow(rows) : null;
            }
        } catch (SQLException e) {
            throw databaseError("load doctor", e);
        }
    }

    public List<Doctor> getAll() {
        List<Doctor> list = new ArrayList<>();
        String sql = "SELECT d.*, u.full_name FROM Doctors d JOIN Users u ON d.user_id=u.user_id "
                + "WHERE u.status='ACTIVE' AND d.diabetes_focus IN ('TYPE_1','TYPE_2','BOTH') "
                + "ORDER BY u.full_name";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) list.add(mapRow(rows));
        } catch (SQLException e) {
            throw databaseError("load doctors", e);
        }
        return list;
    }

    public Doctor create(Doctor d) {
        String sql = "INSERT INTO Doctors(user_id,specialty,license_no,degree,diabetes_focus) VALUES(?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, d.getUserId());
            statement.setString(2, Doctor.DIABETES_SPECIALTY);
            statement.setString(3, d.getLicenseNo());
            statement.setString(4, d.getDegree());
            statement.setString(5, normalizeFocus(d.getDiabetesFocus()));
            if (statement.executeUpdate() > 0) {
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) d.setDoctorId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw databaseError("create doctor", e);
        }
        return d;
    }

    public boolean updateDiabetesFocus(int doctorId, String focus) {
        String sql = "UPDATE doctors SET diabetes_focus=? WHERE doctor_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalizeFocus(focus));
            ps.setInt(2, doctorId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw databaseError("update doctor diabetes focus", e);
        }
    }

    private String normalizeFocus(String focus) {
        return java.util.Set.of("TYPE_1", "TYPE_2", "BOTH").contains(focus)
                ? focus : "BOTH";
    }

    // Cập nhật riêng các giấy tờ (CCCD mặt trước, mặt sau và chứng chỉ hành nghề).
    // Truyền null cho tham số nào không muốn thay đổi (giữ nguyên giá trị cũ trong DB).
    public boolean updateImages(int doctorId, String cccdFrontImagePath,
            String cccdBackImagePath, String licenseImagePath) {
        String sql = "UPDATE Doctors SET "
                + "cccd_image_path = COALESCE(?, cccd_image_path), "
                + "cccd_back_image_path = COALESCE(?, cccd_back_image_path), "
                + "license_image_path = COALESCE(?, license_image_path) "
                + "WHERE doctor_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cccdFrontImagePath);
            statement.setString(2, cccdBackImagePath);
            statement.setString(3, licenseImagePath);
            statement.setInt(4, doctorId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw databaseError("update doctor documents", e);
        }
    }
}
