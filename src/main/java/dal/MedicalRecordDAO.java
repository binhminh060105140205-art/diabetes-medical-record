package dal;

import models.MedicalRecord;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

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
        } catch (SQLException e) { throw databaseError("load medical record", e); }
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
        } catch (SQLException e) { throw databaseError("load patient medical records", e); }
        return list;
    }

    public MedicalRecord getLatestByPatient(int patientId) {
        String sql = "SELECT * FROM MedicalRecords WHERE patient_id=? ORDER BY visit_date DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rows = ps.executeQuery()) { return rows.next() ? mapRow(rows) : null; }
        } catch (SQLException error) { throw databaseError("load latest patient record", error); }
    }

    /** Loads doctor metrics, recent records and pending records in one database round-trip. */
    public DoctorDashboardData loadDoctorDashboard(int doctorId) {
        String sql = """
            WITH stats AS (
              SELECT COUNT(*) total_records,
                     COUNT(*) FILTER (WHERE status='DRAFT') pending_records,
                     (SELECT COUNT(*) FROM patients) total_patients
              FROM medicalrecords WHERE doctor_id=?
            ), items AS (
              SELECT recent.*, 'RECENT' bucket FROM (
                SELECT * FROM medicalrecords WHERE doctor_id=? ORDER BY visit_date DESC LIMIT 5
              ) recent
              UNION ALL
              SELECT pending.*, 'PENDING' bucket FROM (
                SELECT * FROM medicalrecords WHERE doctor_id=? AND status='DRAFT'
                ORDER BY visit_date DESC LIMIT 20
              ) pending
            )
            SELECT i.*,s.total_records,s.pending_records,s.total_patients
            FROM stats s LEFT JOIN items i ON TRUE
            """;
        List<MedicalRecord> recent = new ArrayList<>(), pending = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, doctorId); ps.setInt(2, doctorId); ps.setInt(3, doctorId);
            try (ResultSet rows = ps.executeQuery()) {
                int totalRecords = 0, pendingCount = 0, totalPatients = 0;
                while (rows.next()) {
                    totalRecords = rows.getInt("total_records");
                    pendingCount = rows.getInt("pending_records");
                    totalPatients = rows.getInt("total_patients");
                    if (rows.getObject("record_id") != null) {
                        MedicalRecord record = mapRow(rows);
                        if ("RECENT".equals(rows.getString("bucket"))) recent.add(record);
                        else pending.add(record);
                    }
                }
                return new DoctorDashboardData(totalPatients, totalRecords, pendingCount, recent, pending);
            }
        } catch (SQLException error) { throw databaseError("load doctor dashboard", error); }
    }

    public record DoctorDashboardData(int totalPatients, int totalRecords,
            int pendingCount, List<MedicalRecord> recent, List<MedicalRecord> pending) {}

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
        } catch (SQLException e) { throw databaseError("create medical record", e); }
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
        } catch (SQLException e) { throw databaseError("complete medical record", e); }
    }

    public List<Map<String, Object>> getPrescriptionItems(int recordId) {
        List<Map<String, Object>> items = new ArrayList<>();
        String sql = "SELECT medicine_name,dosage,frequency,duration_days FROM PrescriptionItems "
                + "WHERE record_id=? ORDER BY prescription_item_id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, recordId);
            try (ResultSet rows = ps.executeQuery()) {
                while (rows.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("medicineName", rows.getString("medicine_name"));
                    item.put("dosage", rows.getString("dosage"));
                    item.put("frequency", rows.getString("frequency"));
                    item.put("durationDays", rows.getObject("duration_days"));
                    items.add(item);
                }
            }
        } catch (SQLException e) { throw databaseError("load prescription", e); }
        return items;
    }

    public void replacePrescriptionItems(int recordId, String[] names, String[] dosages,
            String[] frequencies, String[] durations) {
        String delete = "DELETE FROM PrescriptionItems WHERE record_id=?";
        String insert = "INSERT INTO PrescriptionItems(record_id,medicine_name,dosage,frequency,duration_days) "
                + "VALUES(?,?,?,?,?)";
        try (PreparedStatement del = connection.prepareStatement(delete);
             PreparedStatement add = connection.prepareStatement(insert)) {
            del.setInt(1, recordId);
            del.executeUpdate();
            if (names == null) return;
            for (int i = 0; i < names.length; i++) {
                String name = names[i] == null ? "" : names[i].trim();
                if (name.isEmpty()) continue;
                String dosage = valueAt(dosages, i);
                if (dosage.isBlank()) throw new IllegalArgumentException("Thuốc phải có liều dùng.");
                add.setInt(1, recordId);
                add.setString(2, name);
                add.setString(3, dosage);
                add.setString(4, valueAt(frequencies, i));
                String duration = valueAt(durations, i);
                if (duration.isBlank()) add.setNull(5, Types.INTEGER);
                else {
                    int days = Integer.parseInt(duration);
                    if (days < 1 || days > 365) throw new IllegalArgumentException("Số ngày dùng thuốc phải từ 1 đến 365.");
                    add.setInt(5, days);
                }
                add.addBatch();
            }
            add.executeBatch();
        } catch (SQLException e) { throw databaseError("save prescription", e); }
    }

    public void completeWithPrescription(MedicalRecord record, String[] names, String[] dosages,
            String[] frequencies, String[] durations) {
        try {
            connection.setAutoCommit(false);
            updateConclusion(record);
            replacePrescriptionItems(record.getRecordId(), names, dosages, frequencies, durations);
            connection.commit();
        } catch (RuntimeException | SQLException error) {
            try { connection.rollback(); } catch (SQLException rollbackError) { error.addSuppressed(rollbackError); }
            throw error instanceof RuntimeException runtime ? runtime
                    : databaseError("complete record transaction", (SQLException) error);
        } finally {
            try { connection.setAutoCommit(true); }
            catch (SQLException e) { throw databaseError("restore database transaction", e); }
        }
    }

    private String valueAt(String[] values, int index) {
        return values != null && index < values.length && values[index] != null ? values[index].trim() : "";
    }
}
