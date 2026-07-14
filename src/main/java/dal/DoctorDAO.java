package dal;

import models.Doctor;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDAO extends DBContext {
    PreparedStatement stm;
    ResultSet rs;

    private Doctor mapRow(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setDoctorId(rs.getInt("doctor_id"));
        d.setUserId(rs.getInt("user_id"));
        d.setSpecialty(rs.getString("specialty"));
        d.setLicenseNo(rs.getString("license_no"));
        d.setFaceImagePath(rs.getString("face_image_path"));
        d.setCccdImagePath(rs.getString("cccd_image_path"));
        d.setLicenseImagePath(rs.getString("license_image_path"));
        try { d.setFullName(rs.getString("full_name")); } catch (Exception ignored) {}
        return d;
    }

    public Doctor getByUserId(int userId) {
        String sql = "SELECT d.*, u.full_name FROM Doctors d JOIN Users u ON d.user_id=u.user_id WHERE d.user_id=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, userId);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.out.println("DoctorDAO.getByUserId: " + e.getMessage());
        }
        return null;
    }

    public Doctor getById(int doctorId) {
        String sql = "SELECT d.*, u.full_name FROM Doctors d JOIN Users u ON d.user_id=u.user_id WHERE d.doctor_id=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, doctorId);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.out.println("DoctorDAO.getById: " + e.getMessage());
        }
        return null;
    }

    public List<Doctor> getAll() {
        List<Doctor> list = new ArrayList<>();
        String sql = "SELECT d.*, u.full_name FROM Doctors d JOIN Users u ON d.user_id=u.user_id ORDER BY u.full_name";
        try {
            stm = connection.prepareStatement(sql);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("DoctorDAO.getAll: " + e.getMessage());
        }
        return list;
    }

    public Doctor create(Doctor d) {
        String sql = "INSERT INTO Doctors(user_id,specialty,license_no) VALUES(?,?,?)";
        try {
            stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stm.setInt(1, d.getUserId());
            stm.setString(2, d.getSpecialty());
            stm.setString(3, d.getLicenseNo());
            int rows = stm.executeUpdate();
            if (rows > 0) {
                rs = stm.getGeneratedKeys();
                if (rs.next()) d.setDoctorId(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.out.println("DoctorDAO.create ERROR: " + e.getMessage());
        }
        return d;
    }

    // Cập nhật riêng 3 đường dẫn ảnh (khuôn mặt, CCCD, chứng chỉ hành nghề).
    // Truyền null cho tham số nào không muốn thay đổi (giữ nguyên giá trị cũ trong DB).
    public boolean updateImages(int doctorId, String faceImagePath, String cccdImagePath, String licenseImagePath) {
        String sql = "UPDATE Doctors SET "
                + "face_image_path = COALESCE(?, face_image_path), "
                + "cccd_image_path = COALESCE(?, cccd_image_path), "
                + "license_image_path = COALESCE(?, license_image_path) "
                + "WHERE doctor_id = ?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setString(1, faceImagePath);
            stm.setString(2, cccdImagePath);
            stm.setString(3, licenseImagePath);
            stm.setInt(4, doctorId);
            return stm.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("DoctorDAO.updateImages ERROR: " + e.getMessage());
        }
        return false;
    }
}
