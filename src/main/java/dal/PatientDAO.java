package dal;

import models.Patient;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO extends DBContext {
    PreparedStatement stm;
    ResultSet rs;

    private Patient mapRow(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setPatientId(rs.getInt("patient_id"));
        p.setUserId(rs.getInt("user_id"));
        p.setFullName(rs.getString("full_name"));
        Date dob = rs.getDate("date_of_birth");
        if (dob != null) p.setDateOfBirth(dob.toLocalDate());
        p.setGender(rs.getString("gender"));
        p.setPhone(rs.getString("phone"));
        p.setAddress(rs.getString("address"));
        p.setHealthInsuranceNo(rs.getString("health_insurance_no"));
        // Căn cước (có thể chưa có cột nếu chưa chạy migration)
        try {
            p.setNationalId(rs.getString("national_id"));
            Date ndate = rs.getDate("national_id_date");
            if (ndate != null) p.setNationalIdDate(ndate.toLocalDate());
            p.setNationalIdPlace(rs.getString("national_id_place"));
        } catch (SQLException ignored) {}
        p.setCreatedBy(rs.getInt("created_by"));
        return p;
    }

    public List<Patient> getAll() {
        List<Patient> list = new ArrayList<>();
        try {
            stm = connection.prepareStatement("SELECT * FROM Patients ORDER BY created_at DESC");
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("PatientDAO.getAll: " + e.getMessage()); }
        return list;
    }

    public List<Patient> getRecent(int limit) {
        List<Patient> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM Patients ORDER BY created_at DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet result = ps.executeQuery()) { while (result.next()) list.add(mapRow(result)); }
        } catch (SQLException e) { throw new IllegalStateException("Không thể tải bệnh nhân gần đây", e); }
        return list;
    }

    public List<Patient> search(String keyword) {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT * FROM Patients WHERE full_name ILIKE ? OR phone ILIKE ? "
                   + "OR health_insurance_no ILIKE ? OR national_id ILIKE ?";
        try {
            stm = connection.prepareStatement(sql);
            String kw = "%" + keyword + "%";
            stm.setString(1, kw); stm.setString(2, kw);
            stm.setString(3, kw); stm.setString(4, kw);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("PatientDAO.search: " + e.getMessage()); }
        return list;
    }

    public Patient getById(int id) {
        try {
            stm = connection.prepareStatement("SELECT * FROM Patients WHERE patient_id=?");
            stm.setInt(1, id);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.out.println("PatientDAO.getById: " + e.getMessage()); }
        return null;
    }

    public Patient getByUserId(int userId) {
        try {
            stm = connection.prepareStatement("SELECT * FROM Patients WHERE user_id=?");
            stm.setInt(1, userId);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.out.println("PatientDAO.getByUserId: " + e.getMessage()); }
        return null;
    }

    public Patient create(Patient p) {
        String sql = "INSERT INTO Patients(user_id,full_name,date_of_birth,gender,phone,address,"
                   + "health_insurance_no,national_id,national_id_date,national_id_place,created_by)"
                   + " VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try {
            stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (p.getUserId() > 0) stm.setInt(1, p.getUserId());
            else stm.setNull(1, Types.INTEGER);
            stm.setString(2,  p.getFullName());
            stm.setDate(3,    p.getDateOfBirth() != null ? Date.valueOf(p.getDateOfBirth()) : null);
            stm.setString(4,  p.getGender());
            stm.setString(5,  p.getPhone());
            stm.setString(6,  p.getAddress());
            stm.setString(7,  p.getHealthInsuranceNo());
            stm.setString(8,  p.getNationalId());
            stm.setDate(9,    p.getNationalIdDate() != null ? Date.valueOf(p.getNationalIdDate()) : null);
            stm.setString(10, p.getNationalIdPlace());
            stm.setInt(11,    p.getCreatedBy());
            if (stm.executeUpdate() > 0) {
                rs = stm.getGeneratedKeys();
                if (rs.next()) p.setPatientId(rs.getInt(1));
            }
        } catch (SQLException e) { System.out.println("PatientDAO.create: " + e.getMessage()); }
        return p;
    }

    public void update(Patient p) {
        String sql = "UPDATE Patients SET full_name=?,date_of_birth=?,gender=?,phone=?,address=?,"
                   + "health_insurance_no=?,national_id=?,national_id_date=?,national_id_place=?"
                   + " WHERE patient_id=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setString(1,  p.getFullName());
            stm.setDate(2,    p.getDateOfBirth() != null ? Date.valueOf(p.getDateOfBirth()) : null);
            stm.setString(3,  p.getGender());
            stm.setString(4,  p.getPhone());
            stm.setString(5,  p.getAddress());
            stm.setString(6,  p.getHealthInsuranceNo());
            stm.setString(7,  p.getNationalId());
            stm.setDate(8,    p.getNationalIdDate() != null ? Date.valueOf(p.getNationalIdDate()) : null);
            stm.setString(9,  p.getNationalIdPlace());
            stm.setInt(10,    p.getPatientId());
            stm.executeUpdate();
        } catch (SQLException e) { System.out.println("PatientDAO.update: " + e.getMessage()); }
    }

    public void updateBasicProfile(Patient p) throws SQLException {
        String sql = "UPDATE Patients SET full_name=?,date_of_birth=?,gender=?,phone=?,national_id=? WHERE patient_id=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setString(1,  p.getFullName());
            stm.setDate(2,    p.getDateOfBirth() != null ? Date.valueOf(p.getDateOfBirth()) : null);
            stm.setString(3,  p.getGender());
            stm.setString(4,  p.getPhone());
            stm.setString(5,  p.getNationalId());
            stm.setInt(6,    p.getPatientId());
            stm.executeUpdate();
        } catch (SQLException e) {
            System.out.println("PatientDAO.updateBasicProfile failed with national_id: " + e.getMessage());
            try {
                String sqlFallback = "UPDATE Patients SET full_name=?,date_of_birth=?,gender=?,phone=? WHERE patient_id=?";
                stm = connection.prepareStatement(sqlFallback);
                stm.setString(1,  p.getFullName());
                stm.setDate(2,    p.getDateOfBirth() != null ? Date.valueOf(p.getDateOfBirth()) : null);
                stm.setString(3,  p.getGender());
                stm.setString(4,  p.getPhone());
                stm.setInt(5,    p.getPatientId());
                stm.executeUpdate();
            } catch (SQLException ex) {
                System.out.println("PatientDAO.updateBasicProfile fallback failed: " + ex.getMessage());
                throw ex; // Ném ra ngoại lệ để Controller bắt được
            }
        }
    }

    public int countAll() {
        try {
            stm = connection.prepareStatement("SELECT COUNT(*) FROM Patients");
            rs = stm.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.out.println("PatientDAO.countAll: " + e.getMessage()); }
        return 0;
    }

    public List<Patient> getWithPaging(int pageIndex, int pageSize) {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT * FROM Patients ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, pageSize);
            stm.setInt(2, (pageIndex - 1) * pageSize);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("PatientDAO.getWithPaging: " + e.getMessage()); }
        return list;
    }
}
