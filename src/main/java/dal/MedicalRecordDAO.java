package dal;

import models.MedicalRecord;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecordDAO extends DBContext {
    PreparedStatement stm;
    ResultSet rs;

    private MedicalRecord mapRow(ResultSet rs) throws SQLException {
        MedicalRecord r = new MedicalRecord();
        r.setRecordId(rs.getInt("record_id"));
        r.setPatientId(rs.getInt("patient_id"));
        r.setDoctorId(rs.getInt("doctor_id"));
        r.setCreatedByStaff(rs.getInt("created_by_staff"));
        r.setEncounterId(rs.getInt("encounter_id"));
        Timestamp vd = rs.getTimestamp("visit_date");
        if (vd != null) r.setVisitDate(vd.toLocalDateTime());
        r.setReasonForVisit(rs.getString("reason_for_visit"));
        r.setSymptoms(rs.getString("symptoms"));
        r.setMedicalHistory(rs.getString("medical_history"));
        r.setLifestyleHabits(rs.getString("lifestyle_habits"));
        r.setClinicalExam(rs.getString("clinical_exam"));
        r.setComplicationNote(rs.getString("complication_note"));
        r.setFinalDiagnosis(rs.getString("final_diagnosis"));
        r.setTreatmentPlan(rs.getString("treatment_plan"));
        r.setPrescriptionNote(rs.getString("prescription_note"));
        r.setAdvice(rs.getString("advice"));
        Date fup = rs.getDate("follow_up_date");
        if (fup != null) r.setFollowUpDate(fup.toLocalDate());
        r.setDoctorNote(rs.getString("doctor_note"));
        r.setStatus(rs.getString("status"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) r.setCreatedAt(ca.toLocalDateTime());
        return r;
    }

    public MedicalRecord getById(int id) {
        String sql = "SELECT * FROM MedicalRecords WHERE record_id=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, id);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.out.println("MRD.getById: " + e.getMessage()); }
        return null;
    }

    public List<MedicalRecord> getByPatient(int patientId) {
        List<MedicalRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM MedicalRecords WHERE patient_id=? ORDER BY visit_date DESC";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, patientId);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("MRD.getByPatient: " + e.getMessage()); }
        return list;
    }

    public List<MedicalRecord> getByDoctor(int doctorId) {
        List<MedicalRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM MedicalRecords WHERE doctor_id=? ORDER BY visit_date DESC";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, doctorId);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("MRD.getByDoctor: " + e.getMessage()); }
        return list;
    }

    /**
     * Bệnh nhân CHỜ KHÁM: bệnh án DRAFT được giao cho bác sĩ này
     * Staff đã nhập thông tin ban đầu nhưng Doctor chưa kết luận
     */
    public List<MedicalRecord> getPendingByDoctor(int doctorId) {
        List<MedicalRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM MedicalRecords WHERE doctor_id=? AND status='DRAFT' ORDER BY visit_date DESC";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, doctorId);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("MRD.getPending: " + e.getMessage()); }
        return list;
    }

    public MedicalRecord create(MedicalRecord rec) {
        String sql = "INSERT INTO MedicalRecords(patient_id,doctor_id,created_by_staff,encounter_id,"
                   + "reason_for_visit,symptoms,medical_history,lifestyle_habits,clinical_exam,status) "
                   + "VALUES(?,?,?,?,?,?,?,?,?,'DRAFT')";
        try {
            stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stm.setInt(1, rec.getPatientId());
            if (rec.getDoctorId() > 0) stm.setInt(2, rec.getDoctorId()); else stm.setNull(2, Types.INTEGER);
            if (rec.getCreatedByStaff() > 0) stm.setInt(3, rec.getCreatedByStaff()); else stm.setNull(3, Types.INTEGER);
            if (rec.getEncounterId() > 0) stm.setInt(4, rec.getEncounterId()); else stm.setNull(4, Types.INTEGER);
            stm.setString(5, rec.getReasonForVisit());
            stm.setString(6, rec.getSymptoms());
            stm.setString(7, rec.getMedicalHistory());
            stm.setString(8, rec.getLifestyleHabits());
            stm.setString(9, rec.getClinicalExam());
            stm.executeUpdate();
            rs = stm.getGeneratedKeys();
            if (rs.next()) rec.setRecordId(rs.getInt(1));
        } catch (SQLException e) { System.out.println("MRD.create: " + e.getMessage()); }
        return rec;
    }

    public void updateConclusion(MedicalRecord rec) {
        String sql = "UPDATE MedicalRecords SET complication_note=?,final_diagnosis=?,"
                   + "treatment_plan=?,prescription_note=?,advice=?,follow_up_date=?,"
                   + "doctor_note=?,status='COMPLETED' WHERE record_id=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setString(1, rec.getComplicationNote());
            stm.setString(2, rec.getFinalDiagnosis());
            stm.setString(3, rec.getTreatmentPlan());
            stm.setString(4, rec.getPrescriptionNote());
            stm.setString(5, rec.getAdvice());
            stm.setDate(6, rec.getFollowUpDate() != null ? Date.valueOf(rec.getFollowUpDate()) : null);
            stm.setString(7, rec.getDoctorNote());
            stm.setInt(8, rec.getRecordId());
            stm.executeUpdate();
        } catch (SQLException e) { System.out.println("MRD.updateConclusion: " + e.getMessage()); }
    }

    public List<MedicalRecord> getRecent(int limit) {
        List<MedicalRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM MedicalRecords ORDER BY created_at DESC LIMIT ?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, limit);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("MRD.getRecent: " + e.getMessage()); }
        return list;
    }
}
