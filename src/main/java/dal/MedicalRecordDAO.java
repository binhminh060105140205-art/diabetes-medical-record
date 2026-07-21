package dal;

import models.MedicalRecord;
import models.Patient;
import models.Doctor;
import models.HealthIndicator;
import models.DiabetesProfile;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class MedicalRecordDAO extends DBContext {
    private MedicalRecord mapRow(ResultSet rs) throws SQLException {
        MedicalRecord r = new MedicalRecord();
        r.setRecordId(rs.getInt("record_id"));
        r.setPatientId(rs.getInt("patient_id"));
        try { r.setPatientName(rs.getString("patient_name")); } catch (SQLException ignored) { }
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
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? mapRow(rows) : null;
            }
        } catch (SQLException e) { throw databaseError("load medical record", e); }
    }

    /** Loads the patient, access decision and complete visit history in one Aiven round-trip. */
    public PatientHistoryData loadPatientHistory(Integer patientId, Integer patientUserId,
            Integer doctorUserId) {
        String sql = """
            WITH subject AS (
              SELECT p.*,
                     COALESCE(dp.diabetes_type,'UNKNOWN') dp_type,
                     dp.diagnosis_date dp_diagnosis_date,
                     dp.treatment_method dp_treatment_method,
                     dp.hba1c_target dp_hba1c_target,
                     dp.updated_at dp_updated_at,
                     CASE WHEN ? IS NULL THEN TRUE ELSE EXISTS (
                       SELECT 1 FROM doctors d
                       WHERE d.user_id=? AND (
                         EXISTS (SELECT 1 FROM encounters e
                                 WHERE e.doctor_id=d.doctor_id AND e.patient_id=p.patient_id)
                         OR EXISTS (SELECT 1 FROM appointments a
                                    WHERE a.doctor_id=d.doctor_id AND a.patient_id=p.patient_id
                                      AND a.status IN ('BOOKED','CONFIRMED','CHECKED_IN'))
                       )
                     ) END authorized
              FROM patients p
              LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
              WHERE (? IS NOT NULL AND p.patient_id=?)
                 OR (? IS NOT NULL AND p.user_id=?)
              LIMIT 1
            )
            SELECT s.patient_id p_patient_id,s.user_id p_user_id,s.full_name p_full_name,
                   s.date_of_birth p_date_of_birth,s.gender p_gender,s.phone p_phone,
                   s.address p_address,s.health_insurance_no p_health_insurance_no,
                   s.national_id p_national_id,s.national_id_date p_national_id_date,
                   s.national_id_place p_national_id_place,s.created_by p_created_by,
                   s.dp_type,s.dp_diagnosis_date,s.dp_treatment_method,
                   s.dp_hba1c_target,s.dp_updated_at,s.authorized,r.*
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
                DiabetesProfile diabetesProfile = null;
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
                        diabetesProfile = new DiabetesProfile();
                        diabetesProfile.setPatientId(patient.getPatientId());
                        diabetesProfile.setDiabetesType(rows.getString("dp_type"));
                        Date diagnosisDate = rows.getDate("dp_diagnosis_date");
                        if (diagnosisDate != null) diabetesProfile.setDiagnosisDate(diagnosisDate.toLocalDate());
                        diabetesProfile.setTreatmentMethod(rows.getString("dp_treatment_method"));
                        Object target = rows.getObject("dp_hba1c_target");
                        if (target instanceof Number number) diabetesProfile.setHba1cTarget(number.doubleValue());
                        Timestamp updatedAt = rows.getTimestamp("dp_updated_at");
                        if (updatedAt != null) diabetesProfile.setUpdatedAt(updatedAt.toLocalDateTime());
                        authorized = rows.getBoolean("authorized");
                    }
                    if (rows.getObject("record_id") != null) records.add(mapRow(rows));
                }
                return new PatientHistoryData(patient, diabetesProfile, records, authorized);
            }
        } catch (SQLException error) {
            throw databaseError("load patient history", error);
        }
    }

    public record PatientHistoryData(Patient patient, DiabetesProfile diabetesProfile,
            List<MedicalRecord> records,
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
                   dp.diabetes_type dp_type,dp.diagnosis_date dp_diagnosis_date,
                   dp.treatment_method dp_treatment_method,dp.hba1c_target dp_hba1c_target,
                   dp.updated_at dp_updated_at,
                   h.indicator_id h_indicator_id,h.entered_by_staff h_entered_by_staff,
                   h.height h_height,h.weight h_weight,h.bmi h_bmi,h.systolic_bp h_systolic_bp,
                   h.diastolic_bp h_diastolic_bp,h.heart_rate h_heart_rate,
                   h.temperature h_temperature,h.blood_glucose h_blood_glucose,
                   h.hba1c h_hba1c,h.cholesterol h_cholesterol,h.triglyceride h_triglyceride,
                   h.hdl_c h_hdl_c,h.ldl_c h_ldl_c,h.measured_at h_measured_at,
                   lh.indicator_id lh_indicator_id,lh.height lh_height,lh.weight lh_weight,lh.bmi lh_bmi,
                   lh.systolic_bp lh_systolic_bp,lh.diastolic_bp lh_diastolic_bp,
                   lh.heart_rate lh_heart_rate,lh.temperature lh_temperature,
                   lh.blood_glucose lh_blood_glucose,lh.hba1c lh_hba1c,
                   lh.cholesterol lh_cholesterol,lh.triglyceride lh_triglyceride,
                   lh.hdl_c lh_hdl_c,lh.ldl_c lh_ldl_c,lh.measured_at lh_measured_at,
                   (SELECT string_agg(l.test_name||': '||COALESCE(l.result_value,'Chưa có kết quả')||
                           CASE WHEN l.result_unit IS NULL THEN '' ELSE ' '||l.result_unit END,' | ' ORDER BY l.ordered_at)
                    FROM lab_orders l WHERE l.encounter_id=r.encounter_id) lab_summary,
                   d.user_id d_user_id,d.specialty d_specialty,d.license_no d_license_no,
                   d.face_image_path d_face_image_path,d.cccd_image_path d_cccd_image_path,
                   d.cccd_back_image_path d_cccd_back_image_path,
                   d.license_image_path d_license_image_path,u.full_name d_full_name,
                   pi.prescription_item_id pi_id,pi.medicine_name pi_name,pi.dosage pi_dosage,
                   pi.frequency pi_frequency,pi.duration_days pi_duration_days
            FROM medicalrecords r
            JOIN patients p ON p.patient_id=r.patient_id
            LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
            LEFT JOIN healthindicators h ON h.record_id=r.record_id
            LEFT JOIN LATERAL (
              SELECT hi.* FROM healthindicators hi
              JOIN medicalrecords mr ON mr.record_id=hi.record_id
              WHERE mr.patient_id=r.patient_id
              ORDER BY hi.measured_at DESC LIMIT 1
            ) lh ON TRUE
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
                HealthIndicator latestIndicator = null;
                DiabetesProfile diabetesProfile = null;
                String labSummary = null;
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
                        diabetesProfile = new DiabetesProfile();
                        diabetesProfile.setPatientId(record.getPatientId());
                        diabetesProfile.setDiabetesType(rows.getString("dp_type"));
                        Date diagnosisDate = rows.getDate("dp_diagnosis_date");
                        if (diagnosisDate != null) diabetesProfile.setDiagnosisDate(diagnosisDate.toLocalDate());
                        diabetesProfile.setTreatmentMethod(rows.getString("dp_treatment_method"));
                        Object target = rows.getObject("dp_hba1c_target");
                        if (target instanceof Number number) diabetesProfile.setHba1cTarget(number.doubleValue());
                        Timestamp profileUpdatedAt = rows.getTimestamp("dp_updated_at");
                        if (profileUpdatedAt != null) diabetesProfile.setUpdatedAt(profileUpdatedAt.toLocalDateTime());
                        labSummary = rows.getString("lab_summary");
                        if (rows.getObject("d_user_id") != null) {
                            doctor = new Doctor();
                            doctor.setDoctorId(record.getDoctorId());
                            doctor.setUserId(rows.getInt("d_user_id"));
                            doctor.setFullName(rows.getString("d_full_name"));
                            doctor.setSpecialty(rows.getString("d_specialty"));
                            doctor.setLicenseNo(rows.getString("d_license_no"));
                            doctor.setFaceImagePath(rows.getString("d_face_image_path"));
                            doctor.setCccdImagePath(rows.getString("d_cccd_image_path"));
                            doctor.setCccdBackImagePath(rows.getString("d_cccd_back_image_path"));
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
                            indicator.setBloodGlucose(nullableDouble(rows, "h_blood_glucose"));
                            indicator.setHba1c(nullableDouble(rows, "h_hba1c"));
                            indicator.setCholesterol(nullableDouble(rows, "h_cholesterol"));
                            indicator.setTriglyceride(nullableDouble(rows, "h_triglyceride"));
                            indicator.setHdlC(nullableDouble(rows, "h_hdl_c"));
                            indicator.setLdlC(nullableDouble(rows, "h_ldl_c"));
                            Timestamp measuredAt = rows.getTimestamp("h_measured_at");
                            if (measuredAt != null) indicator.setMeasuredAt(measuredAt.toLocalDateTime());
                        }
                        if (rows.getObject("lh_indicator_id") != null) {
                            latestIndicator = new HealthIndicator();
                            latestIndicator.setIndicatorId(rows.getInt("lh_indicator_id"));
                            latestIndicator.setHeight(rows.getDouble("lh_height"));
                            latestIndicator.setWeight(rows.getDouble("lh_weight"));
                            latestIndicator.setBmi(rows.getDouble("lh_bmi"));
                            latestIndicator.setSystolicBp(rows.getInt("lh_systolic_bp"));
                            latestIndicator.setDiastolicBp(rows.getInt("lh_diastolic_bp"));
                            latestIndicator.setHeartRate(rows.getInt("lh_heart_rate"));
                            latestIndicator.setTemperature(rows.getDouble("lh_temperature"));
                            latestIndicator.setBloodGlucose(nullableDouble(rows, "lh_blood_glucose"));
                            latestIndicator.setHba1c(nullableDouble(rows, "lh_hba1c"));
                            latestIndicator.setCholesterol(nullableDouble(rows, "lh_cholesterol"));
                            latestIndicator.setTriglyceride(nullableDouble(rows, "lh_triglyceride"));
                            latestIndicator.setHdlC(nullableDouble(rows, "lh_hdl_c"));
                            latestIndicator.setLdlC(nullableDouble(rows, "lh_ldl_c"));
                            Timestamp latestMeasuredAt = rows.getTimestamp("lh_measured_at");
                            if (latestMeasuredAt != null) latestIndicator.setMeasuredAt(latestMeasuredAt.toLocalDateTime());
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
                List<Map<String, Object>> labOrders = record == null
                        ? new ArrayList<>() : loadLabOrders(record.getEncounterId());
                return new MedicalRecordFormData(record, patient, doctor, indicator, latestIndicator,
                        diabetesProfile, labSummary, prescriptions, labOrders);
            }
        } catch (SQLException error) {
            throw databaseError("load medical record form", error);
        }
    }

    public record MedicalRecordFormData(MedicalRecord record, Patient patient, Doctor doctor,
            HealthIndicator indicator, HealthIndicator latestIndicator, DiabetesProfile diabetesProfile,
            String labSummary, List<Map<String, Object>> prescriptionItems,
            List<Map<String, Object>> labOrders) {}

    private Double nullableDouble(ResultSet rows, String column) throws SQLException {
        double value = rows.getDouble(column);
        return rows.wasNull() ? null : value;
    }

    private List<Map<String, Object>> loadLabOrders(int encounterId) throws SQLException {
        List<Map<String, Object>> orders = new ArrayList<>();
        if (encounterId <= 0) return orders;
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT lab_order_id,encounter_id,test_code,test_name,status,priority,
                       result_value,result_unit,reference_range,result_flag,ordered_at,resulted_at
                FROM lab_orders WHERE encounter_id=? ORDER BY ordered_at,lab_order_id""")) {
            ps.setInt(1, encounterId);
            try (ResultSet rows = ps.executeQuery()) {
                while (rows.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("lab_order_id", rows.getInt("lab_order_id"));
                    item.put("encounter_id", rows.getInt("encounter_id"));
                    item.put("test_code", rows.getString("test_code"));
                    item.put("test_name", rows.getString("test_name"));
                    item.put("status", rows.getString("status"));
                    item.put("priority", rows.getString("priority"));
                    item.put("result_value", rows.getString("result_value"));
                    item.put("result_unit", rows.getString("result_unit"));
                    item.put("reference_range", rows.getString("reference_range"));
                    item.put("result_flag", rows.getString("result_flag"));
                    orders.add(item);
                }
            }
        }
        return orders;
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
                SELECT r.*,p.full_name patient_name
                FROM medicalrecords r JOIN selected s ON s.doctor_id=r.doctor_id
                JOIN patients p ON p.patient_id=r.patient_id
                ORDER BY r.visit_date DESC LIMIT 5
              ) recent
              UNION ALL
              SELECT pending.*, 'PENDING' bucket FROM (
                SELECT r.*,p.full_name patient_name
                FROM medicalrecords r JOIN selected s ON s.doctor_id=r.doctor_id
                JOIN patients p ON p.patient_id=r.patient_id
                WHERE r.status='DRAFT' ORDER BY r.visit_date DESC LIMIT 20
              ) pending
            )
            SELECT i.*,s.doctor_id selected_doctor_id,s.user_id selected_user_id,
                   s.specialty selected_specialty,s.license_no selected_license_no,
                   s.degree selected_degree,
                   s.face_image_path selected_face_image_path,
                   s.cccd_image_path selected_cccd_image_path,
                   s.cccd_back_image_path selected_cccd_back_image_path,
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
                        doctor.setDegree(rows.getString("selected_degree"));
                        doctor.setFaceImagePath(rows.getString("selected_face_image_path"));
                        doctor.setCccdImagePath(rows.getString("selected_cccd_image_path"));
                        doctor.setCccdBackImagePath(rows.getString("selected_cccd_back_image_path"));
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
                   + "VALUES(?,?,?,?,?,?,?,?,?,'DRAFT') "
                   + "ON CONFLICT(encounter_id) DO UPDATE SET encounter_id=EXCLUDED.encounter_id "
                   + "RETURNING record_id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, rec.getPatientId());
            setPositiveOrNull(statement, 2, rec.getDoctorId());
            setPositiveOrNull(statement, 3, rec.getCreatedByStaff());
            setPositiveOrNull(statement, 4, rec.getEncounterId());
            statement.setString(5, rec.getReasonForVisit());
            statement.setString(6, rec.getSymptoms());
            statement.setString(7, rec.getMedicalHistory());
            statement.setString(8, rec.getLifestyleHabits());
            statement.setString(9, rec.getClinicalExam());
            try (ResultSet keys = statement.executeQuery()) {
                if (keys.next()) rec.setRecordId(keys.getInt("record_id"));
            }
        } catch (SQLException e) { throw databaseError("create medical record", e); }
        return rec;
    }

    public void updateConclusion(MedicalRecord rec) {
        String sql = "UPDATE MedicalRecords SET complication_note=?,final_diagnosis=?,"
                   + "treatment_plan=?,prescription_note=?,advice=?,follow_up_date=?,"
                   + "doctor_note=?,status='COMPLETED' WHERE record_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, rec.getComplicationNote());
            statement.setString(2, rec.getFinalDiagnosis());
            statement.setString(3, rec.getTreatmentPlan());
            statement.setString(4, rec.getPrescriptionNote());
            statement.setString(5, rec.getAdvice());
            statement.setDate(6, rec.getFollowUpDate() != null
                    ? Date.valueOf(rec.getFollowUpDate()) : null);
            statement.setString(7, rec.getDoctorNote());
            statement.setInt(8, rec.getRecordId());
            statement.executeUpdate();
        } catch (SQLException e) { throw databaseError("complete medical record", e); }
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
                if (name.length() > 150) {
                    throw new IllegalArgumentException("Tên thuốc tối đa 150 ký tự.");
                }
                String dosage = valueAt(dosages, i);
                if (dosage.isBlank()) throw new IllegalArgumentException("Thuốc phải có liều dùng.");
                if (dosage.length() > 100) {
                    throw new IllegalArgumentException("Liều dùng tối đa 100 ký tự.");
                }
                String frequency = valueAt(frequencies, i);
                if (frequency.isBlank()) {
                    throw new IllegalArgumentException("Thuốc phải có số lần dùng trong ngày.");
                }
                if (frequency.length() > 100) {
                    throw new IllegalArgumentException("Tần suất dùng thuốc tối đa 100 ký tự.");
                }
                add.setInt(1, recordId);
                add.setString(2, name);
                add.setString(3, dosage);
                add.setString(4, frequency);
                String duration = valueAt(durations, i);
                if (duration.isBlank()) {
                    throw new IllegalArgumentException("Thuốc phải có số ngày sử dụng.");
                }
                int days = Integer.parseInt(duration);
                if (days < 1 || days > 365) throw new IllegalArgumentException("Số ngày dùng thuốc phải từ 1 đến 365.");
                add.setInt(5, days);
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

    private void setPositiveOrNull(PreparedStatement statement, int index, int value)
            throws SQLException {
        if (value > 0) statement.setInt(index, value);
        else statement.setNull(index, Types.INTEGER);
    }
}
