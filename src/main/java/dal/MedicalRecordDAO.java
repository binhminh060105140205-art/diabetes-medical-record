package dal;

import models.MedicalRecord;
import models.Patient;
import models.Doctor;
import models.HealthIndicator;
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

    /** Loads the patient, access decision and complete visit history in one Aiven round-trip. */
    public PatientHistoryData loadPatientHistory(Integer patientId, Integer patientUserId,
            Integer doctorUserId) {
        String sql = """
            WITH subject AS (
              SELECT p.*,
                     CASE WHEN ? IS NULL THEN TRUE ELSE EXISTS (
                       SELECT 1 FROM doctors d JOIN encounters e ON e.doctor_id=d.doctor_id
                       WHERE d.user_id=? AND e.patient_id=p.patient_id
                     ) END authorized
              FROM patients p
              WHERE (? IS NOT NULL AND p.patient_id=?)
                 OR (? IS NOT NULL AND p.user_id=?)
              LIMIT 1
            )
            SELECT s.patient_id p_patient_id,s.user_id p_user_id,s.full_name p_full_name,
                   s.date_of_birth p_date_of_birth,s.gender p_gender,s.phone p_phone,
                   s.address p_address,s.health_insurance_no p_health_insurance_no,
                   s.national_id p_national_id,s.national_id_date p_national_id_date,
                   s.national_id_place p_national_id_place,s.created_by p_created_by,
                   s.authorized,r.*
            FROM subject s LEFT JOIN medicalrecords r ON r.patient_id=s.patient_id
            ORDER BY r.visit_date DESC
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, doctorUserId, Types.INTEGER);
            ps.setObject(2, doctorUserId, Types.INTEGER);
            ps.setObject(3, patientId, Types.INTEGER);
            ps.setObject(4, patientId, Types.INTEGER);
            ps.setObject(5, patientUserId, Types.INTEGER);
            ps.setObject(6, patientUserId, Types.INTEGER);
            try (ResultSet rows = ps.executeQuery()) {
                Patient patient = null;
                boolean authorized = doctorUserId == null;
                List<MedicalRecord> records = new ArrayList<>();
                while (rows.next()) {
                    if (patient == null) {
                        patient = new Patient();
                        patient.setPatientId(rows.getInt("p_patient_id"));
                        patient.setUserId(rows.getInt("p_user_id"));
                        patient.setFullName(rows.getString("p_full_name"));
                        Date dob = rows.getDate("p_date_of_birth");
                        if (dob != null) patient.setDateOfBirth(dob.toLocalDate());
                        patient.setGender(rows.getString("p_gender"));
                        patient.setPhone(rows.getString("p_phone"));
                        patient.setAddress(rows.getString("p_address"));
                        patient.setHealthInsuranceNo(rows.getString("p_health_insurance_no"));
                        patient.setNationalId(rows.getString("p_national_id"));
                        Date nationalIdDate = rows.getDate("p_national_id_date");
                        if (nationalIdDate != null) patient.setNationalIdDate(nationalIdDate.toLocalDate());
                        patient.setNationalIdPlace(rows.getString("p_national_id_place"));
                        patient.setCreatedBy(rows.getInt("p_created_by"));
                        authorized = rows.getBoolean("authorized");
                    }
                    if (rows.getObject("record_id") != null) records.add(mapRow(rows));
                }
                return new PatientHistoryData(patient, records, authorized);
            }
        } catch (SQLException error) {
            throw databaseError("load patient history", error);
        }
    }

    public record PatientHistoryData(Patient patient, List<MedicalRecord> records,
            boolean authorized) {}

    /** Loads every read-only section of the medical-record form in one database round-trip. */
    public MedicalRecordFormData loadFormData(int recordId) {
        String sql = """
            SELECT r.*,
                   p.user_id p_user_id,p.full_name p_full_name,p.date_of_birth p_date_of_birth,
                   p.gender p_gender,p.phone p_phone,p.address p_address,
                   p.health_insurance_no p_health_insurance_no,p.national_id p_national_id,
                   p.national_id_date p_national_id_date,p.national_id_place p_national_id_place,
                   p.created_by p_created_by,
                   h.indicator_id h_indicator_id,h.entered_by_staff h_entered_by_staff,
                   h.height h_height,h.weight h_weight,h.bmi h_bmi,h.systolic_bp h_systolic_bp,
                   h.diastolic_bp h_diastolic_bp,h.heart_rate h_heart_rate,
                   h.temperature h_temperature,h.blood_glucose h_blood_glucose,
                   h.hba1c h_hba1c,h.cholesterol h_cholesterol,h.triglyceride h_triglyceride,
                   h.hdl_c h_hdl_c,h.ldl_c h_ldl_c,h.measured_at h_measured_at,
                   d.user_id d_user_id,d.specialty d_specialty,d.license_no d_license_no,
                   d.face_image_path d_face_image_path,d.cccd_image_path d_cccd_image_path,
                   d.license_image_path d_license_image_path,u.full_name d_full_name,
                   pi.prescription_item_id pi_id,pi.medicine_name pi_name,pi.dosage pi_dosage,
                   pi.frequency pi_frequency,pi.duration_days pi_duration_days
            FROM medicalrecords r
            JOIN patients p ON p.patient_id=r.patient_id
            LEFT JOIN healthindicators h ON h.record_id=r.record_id
            LEFT JOIN doctors d ON d.doctor_id=r.doctor_id
            LEFT JOIN users u ON u.user_id=d.user_id
            LEFT JOIN prescriptionitems pi ON pi.record_id=r.record_id
            WHERE r.record_id=? ORDER BY pi.prescription_item_id
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, recordId);
            try (ResultSet rows = ps.executeQuery()) {
                MedicalRecord record = null;
                Patient patient = null;
                Doctor doctor = null;
                HealthIndicator indicator = null;
                List<Map<String, Object>> prescriptions = new ArrayList<>();
                while (rows.next()) {
                    if (record == null) {
                        record = mapRow(rows);
                        patient = new Patient();
                        patient.setPatientId(record.getPatientId());
                        patient.setUserId(rows.getInt("p_user_id"));
                        patient.setFullName(rows.getString("p_full_name"));
                        Date dob = rows.getDate("p_date_of_birth");
                        if (dob != null) patient.setDateOfBirth(dob.toLocalDate());
                        patient.setGender(rows.getString("p_gender"));
                        patient.setPhone(rows.getString("p_phone"));
                        patient.setAddress(rows.getString("p_address"));
                        patient.setHealthInsuranceNo(rows.getString("p_health_insurance_no"));
                        patient.setNationalId(rows.getString("p_national_id"));
                        Date nationalIdDate = rows.getDate("p_national_id_date");
                        if (nationalIdDate != null) patient.setNationalIdDate(nationalIdDate.toLocalDate());
                        patient.setNationalIdPlace(rows.getString("p_national_id_place"));
                        patient.setCreatedBy(rows.getInt("p_created_by"));
                        if (rows.getObject("d_user_id") != null) {
                            doctor = new Doctor();
                            doctor.setDoctorId(record.getDoctorId());
                            doctor.setUserId(rows.getInt("d_user_id"));
                            doctor.setFullName(rows.getString("d_full_name"));
                            doctor.setSpecialty(rows.getString("d_specialty"));
                            doctor.setLicenseNo(rows.getString("d_license_no"));
                            doctor.setFaceImagePath(rows.getString("d_face_image_path"));
                            doctor.setCccdImagePath(rows.getString("d_cccd_image_path"));
                            doctor.setLicenseImagePath(rows.getString("d_license_image_path"));
                        }
                        if (rows.getObject("h_indicator_id") != null) {
                            indicator = new HealthIndicator();
                            indicator.setIndicatorId(rows.getInt("h_indicator_id"));
                            indicator.setRecordId(recordId);
                            indicator.setEnteredByStaff(rows.getInt("h_entered_by_staff"));
                            indicator.setHeight(rows.getDouble("h_height"));
                            indicator.setWeight(rows.getDouble("h_weight"));
                            indicator.setBmi(rows.getDouble("h_bmi"));
                            indicator.setSystolicBp(rows.getInt("h_systolic_bp"));
                            indicator.setDiastolicBp(rows.getInt("h_diastolic_bp"));
                            indicator.setHeartRate(rows.getInt("h_heart_rate"));
                            indicator.setTemperature(rows.getDouble("h_temperature"));
                            indicator.setBloodGlucose(rows.getDouble("h_blood_glucose"));
                            indicator.setHba1c(rows.getDouble("h_hba1c"));
                            indicator.setCholesterol(rows.getDouble("h_cholesterol"));
                            indicator.setTriglyceride(rows.getDouble("h_triglyceride"));
                            indicator.setHdlC(rows.getDouble("h_hdl_c"));
                            indicator.setLdlC(rows.getDouble("h_ldl_c"));
                            Timestamp measuredAt = rows.getTimestamp("h_measured_at");
                            if (measuredAt != null) indicator.setMeasuredAt(measuredAt.toLocalDateTime());
                        }
                    }
                    if (rows.getObject("pi_id") != null) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("medicineName", rows.getString("pi_name"));
                        item.put("dosage", rows.getString("pi_dosage"));
                        item.put("frequency", rows.getString("pi_frequency"));
                        item.put("durationDays", rows.getObject("pi_duration_days"));
                        prescriptions.add(item);
                    }
                }
                return new MedicalRecordFormData(record, patient, doctor, indicator, prescriptions);
            }
        } catch (SQLException error) {
            throw databaseError("load medical record form", error);
        }
    }

    public record MedicalRecordFormData(MedicalRecord record, Patient patient, Doctor doctor,
            HealthIndicator indicator, List<Map<String, Object>> prescriptionItems) {}

    public MedicalRecord getLatestByPatient(int patientId) {
        String sql = "SELECT * FROM MedicalRecords WHERE patient_id=? ORDER BY visit_date DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rows = ps.executeQuery()) { return rows.next() ? mapRow(rows) : null; }
        } catch (SQLException error) { throw databaseError("load latest patient record", error); }
    }

    /** Loads doctor identity, metrics and dashboard records in one database round-trip. */
    public DoctorDashboardData loadDoctorDashboardForUser(int userId) {
        String sql = """
            WITH selected AS (
              SELECT d.*,u.full_name FROM doctors d JOIN users u ON u.user_id=d.user_id
              WHERE d.user_id=?
            ), stats AS (
              SELECT s.doctor_id,
                     COUNT(r.record_id) total_records,
                     COUNT(r.record_id) FILTER (WHERE r.status='DRAFT') pending_records,
                     (SELECT COUNT(*) FROM patients) total_patients
              FROM selected s LEFT JOIN medicalrecords r ON r.doctor_id=s.doctor_id
              GROUP BY s.doctor_id
            ), items AS (
              SELECT recent.*, 'RECENT' bucket FROM (
                SELECT r.* FROM medicalrecords r JOIN selected s ON s.doctor_id=r.doctor_id
                ORDER BY r.visit_date DESC LIMIT 5
              ) recent
              UNION ALL
              SELECT pending.*, 'PENDING' bucket FROM (
                SELECT r.* FROM medicalrecords r JOIN selected s ON s.doctor_id=r.doctor_id
                WHERE r.status='DRAFT' ORDER BY r.visit_date DESC LIMIT 20
              ) pending
            )
            SELECT i.*,s.doctor_id selected_doctor_id,s.user_id selected_user_id,
                   s.specialty selected_specialty,s.license_no selected_license_no,
                   s.face_image_path selected_face_image_path,
                   s.cccd_image_path selected_cccd_image_path,
                   s.license_image_path selected_license_image_path,
                   s.full_name selected_full_name,
                   st.total_records,st.pending_records,st.total_patients
            FROM selected s JOIN stats st ON st.doctor_id=s.doctor_id
            LEFT JOIN items i ON TRUE
            """;
        List<MedicalRecord> recent = new ArrayList<>(), pending = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rows = ps.executeQuery()) {
                Doctor doctor = null;
                int totalRecords = 0, pendingCount = 0, totalPatients = 0;
                while (rows.next()) {
                    if (doctor == null) {
                        doctor = new Doctor();
                        doctor.setDoctorId(rows.getInt("selected_doctor_id"));
                        doctor.setUserId(rows.getInt("selected_user_id"));
                        doctor.setFullName(rows.getString("selected_full_name"));
                        doctor.setSpecialty(rows.getString("selected_specialty"));
                        doctor.setLicenseNo(rows.getString("selected_license_no"));
                        doctor.setFaceImagePath(rows.getString("selected_face_image_path"));
                        doctor.setCccdImagePath(rows.getString("selected_cccd_image_path"));
                        doctor.setLicenseImagePath(rows.getString("selected_license_image_path"));
                    }
                    totalRecords = rows.getInt("total_records");
                    pendingCount = rows.getInt("pending_records");
                    totalPatients = rows.getInt("total_patients");
                    if (rows.getObject("record_id") != null) {
                        MedicalRecord record = mapRow(rows);
                        if ("RECENT".equals(rows.getString("bucket"))) recent.add(record);
                        else pending.add(record);
                    }
                }
                return new DoctorDashboardData(doctor, totalPatients, totalRecords,
                        pendingCount, recent, pending);
            }
        } catch (SQLException error) { throw databaseError("load doctor dashboard", error); }
    }

    public record DoctorDashboardData(Doctor doctor, int totalPatients, int totalRecords,
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
