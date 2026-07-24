package dal;

import models.Patient;
import models.MedicalRecord;
import models.PatientDailyLog;
import models.DiabetesProfile;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO extends DBContext {
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
        p.setNationalId(rs.getString("national_id"));
        Date ndate = rs.getDate("national_id_date");
        if (ndate != null) p.setNationalIdDate(ndate.toLocalDate());
        p.setNationalIdPlace(rs.getString("national_id_place"));
        p.setCreatedBy(rs.getInt("created_by"));
        return p;
    }

    /** Lightweight rows for patient selectors; avoids loading private profile columns. */
    public List<Patient> listForSelection() {
        List<Patient> list = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT p.patient_id,p.full_name,p.phone,COALESCE(dp.diabetes_type,'UNKNOWN') diabetes_type "
                + "FROM patients p LEFT JOIN users u ON u.user_id=p.user_id "
                + "LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id "
                + "WHERE COALESCE(u.status,'ACTIVE') <> 'DELETED' ORDER BY p.full_name");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                Patient patient = new Patient();
                patient.setPatientId(rows.getInt("patient_id"));
                patient.setFullName(rows.getString("full_name"));
                patient.setPhone(rows.getString("phone"));
                patient.setDiabetesType(rows.getString("diabetes_type"));
                list.add(patient);
            }
        } catch (SQLException e) { throw databaseError("load patients", e); }
        return list;
    }

    /** Only exposes patients who have been assigned to the signed-in doctor. */
    public List<Patient> listForDoctorSelection(int doctorUserId) {
        List<Patient> list = new ArrayList<>();
        String sql = """
                SELECT DISTINCT p.patient_id,p.full_name,p.phone,
                       COALESCE(dp.diabetes_type,'UNKNOWN') diabetes_type
                FROM patients p
                LEFT JOIN users u ON u.user_id=p.user_id
                LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
                JOIN doctors d ON d.user_id=?
                WHERE COALESCE(u.status,'ACTIVE') <> 'DELETED'
                  AND (
                    EXISTS (SELECT 1 FROM encounters e
                            WHERE e.doctor_id=d.doctor_id AND e.patient_id=p.patient_id)
                    OR EXISTS (SELECT 1 FROM appointments a
                               WHERE a.doctor_id=d.doctor_id AND a.patient_id=p.patient_id
                                 AND a.status IN ('BOOKED','CONFIRMED','CHECKED_IN'))
                  )
                ORDER BY p.full_name
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, doctorUserId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Patient patient = new Patient();
                    patient.setPatientId(rows.getInt("patient_id"));
                    patient.setFullName(rows.getString("full_name"));
                    patient.setPhone(rows.getString("phone"));
                    patient.setDiabetesType(rows.getString("diabetes_type"));
                    list.add(patient);
                }
            }
        } catch (SQLException error) {
            throw databaseError("load assigned patients", error);
        }
        return list;
    }

    /** Loads total patient count and the dashboard list in one database round-trip. */
    public StaffDashboardData loadStaffDashboard(String keyword, int limit) {
        boolean searching = keyword != null && !keyword.isBlank();
        String where = searching
                ? " WHERE COALESCE(u.status,'ACTIVE') <> 'DELETED' AND "
                  + "(" + SearchSupport.contains("p.full_name")
                  + " OR LOWER(p.phone) LIKE LOWER(?) OR LOWER(p.health_insurance_no) LIKE LOWER(?) OR LOWER(p.national_id) LIKE LOWER(?))"
                : " WHERE COALESCE(u.status,'ACTIVE') <> 'DELETED'";
        String order = searching ? " ORDER BY p.full_name" : " ORDER BY p.created_at DESC";
        String sql = "WITH total AS (SELECT COUNT(*) value FROM patients p LEFT JOIN users u ON u.user_id=p.user_id"
                + " WHERE COALESCE(u.status,'ACTIVE') <> 'DELETED'), data AS (SELECT p.* FROM patients p "
                + "LEFT JOIN users u ON u.user_id=p.user_id"
                + where + order + " OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY) SELECT d.*,t.value total FROM total t LEFT JOIN data d ON 1=1";
        List<Patient> patients = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            if (searching) {
                String value = "%" + keyword.trim() + "%";
                for (int i = 0; i < 4; i++) ps.setString(index++, value);
            }
            ps.setInt(index, limit);
            try (ResultSet rows = ps.executeQuery()) {
                int total = 0;
                while (rows.next()) {
                    total = rows.getInt("total");
                    if (rows.getObject("patient_id") != null) patients.add(mapRow(rows));
                }
                return new StaffDashboardData(total, patients);
            }
        } catch (SQLException error) { throw databaseError("load staff dashboard", error); }
    }

    public record StaffDashboardData(int totalPatients, List<Patient> patients) {}

    /** Loads a filtered count and one page in one database round-trip. */
    public PatientListData loadPatientList(String keyword, int page, int pageSize) {
        boolean searching = keyword != null && !keyword.isBlank();
        String where = searching
                ? " WHERE COALESCE(u.status,'ACTIVE') <> 'DELETED' AND "
                  + "(" + SearchSupport.contains("p.full_name")
                  + " OR LOWER(p.phone) LIKE LOWER(?) OR LOWER(p.health_insurance_no) LIKE LOWER(?) OR LOWER(p.national_id) LIKE LOWER(?))"
                : " WHERE COALESCE(u.status,'ACTIVE') <> 'DELETED'";
        // The outer CTE query exposes patient columns without the original p alias.
        String order = searching ? " ORDER BY full_name" : " ORDER BY created_at DESC";
        String sql = "WITH filtered AS (SELECT p.* FROM patients p LEFT JOIN users u ON u.user_id=p.user_id"
                + where + "), total AS (SELECT COUNT(*) value FROM filtered), "
                + "pending_requests AS (SELECT COUNT(*) value FROM appointments WHERE status='REQUESTED'), "
                + "data AS (SELECT * FROM filtered" + order + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY) "
                + "SELECT d.*,t.value total,pr.value pending_appointment_requests "
                + "FROM total t CROSS JOIN pending_requests pr LEFT JOIN data d ON 1=1";
        List<Patient> patients = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            if (searching) {
                String value = "%" + keyword.trim() + "%";
                for (int i = 0; i < 4; i++) ps.setString(index++, value);
            }
            ps.setInt(index++, (page - 1) * pageSize);
            ps.setInt(index, pageSize);
            try (ResultSet rows = ps.executeQuery()) {
                int total = 0;
                int pendingAppointmentRequests = 0;
                while (rows.next()) {
                    total = rows.getInt("total");
                    pendingAppointmentRequests = rows.getInt("pending_appointment_requests");
                    if (rows.getObject("patient_id") != null) patients.add(mapRow(rows));
                }
                return new PatientListData(total, patients, pendingAppointmentRequests);
            }
        } catch (SQLException error) { throw databaseError("load patient list", error); }
    }

    public record PatientListData(int total, List<Patient> patients,
            int pendingAppointmentRequests) {}

    /** Paginated patient list scoped to one doctor, including past encounters. */
    public PatientListData loadPatientListForDoctor(int doctorUserId, String keyword,
            int page, int pageSize) {
        boolean searching = keyword != null && !keyword.isBlank();
        String search = searching
                ? " AND (" + SearchSupport.contains("p.full_name") + " OR LOWER(p.phone) LIKE LOWER(?) OR "
                  + "LOWER(p.health_insurance_no) LIKE LOWER(?) OR LOWER(p.national_id) LIKE LOWER(?))"
                : "";
        String order = searching ? " ORDER BY full_name" : " ORDER BY created_at DESC";
        String sql = """
                WITH doctor_scope AS (
                  SELECT doctor_id FROM doctors WHERE user_id=?
                ), filtered AS (
                  SELECT p.* FROM patients p
                  LEFT JOIN users u ON u.user_id=p.user_id
                  JOIN doctor_scope d ON 1=1
                  WHERE COALESCE(u.status,'ACTIVE') <> 'DELETED'
                    AND (
                      EXISTS (SELECT 1 FROM encounters e
                              WHERE e.doctor_id=d.doctor_id AND e.patient_id=p.patient_id)
                      OR EXISTS (SELECT 1 FROM appointments a
                                 WHERE a.doctor_id=d.doctor_id AND a.patient_id=p.patient_id
                                   AND a.status IN ('BOOKED','CONFIRMED','CHECKED_IN'))
                    )
                """ + search + """
                ), total AS (SELECT COUNT(*) value FROM filtered),
                data AS (SELECT * FROM filtered
                """ + order + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY) "
                + "SELECT d.*,t.value total FROM total t LEFT JOIN data d ON 1=1";
        List<Patient> patients = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            statement.setInt(index++, doctorUserId);
            if (searching) {
                String value = "%" + keyword.trim() + "%";
                for (int i = 0; i < 4; i++) statement.setString(index++, value);
            }
            statement.setInt(index++, (page - 1) * pageSize);
            statement.setInt(index, pageSize);
            try (ResultSet rows = statement.executeQuery()) {
                int total = 0;
                while (rows.next()) {
                    total = rows.getInt("total");
                    if (rows.getObject("patient_id") != null) patients.add(mapRow(rows));
                }
                return new PatientListData(total, patients, 0);
            }
        } catch (SQLException error) {
            throw databaseError("load doctor patient list", error);
        }
    }

    public Patient getById(int id) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM Patients WHERE patient_id=?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? mapRow(rows) : null;
            }
        } catch (SQLException e) { throw databaseError("load patient", e); }
    }

    public Patient getByUserId(int userId) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM Patients WHERE user_id=?")) {
            statement.setInt(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? mapRow(rows) : null;
            }
        } catch (SQLException e) { throw databaseError("load patient account", e); }
    }

    /** Loads every read-only section of the patient home page in one database round-trip. */
    public PatientDashboardData loadPatientDashboard(int userId) {
        String sql = """
            SELECT p.*,
                   COALESCE(dp.diabetes_type,'UNKNOWN') dp_type,
                   dp.diagnosis_date dp_diagnosis_date,
                   dp.treatment_method dp_treatment_method,
                   dp.hba1c_target dp_hba1c_target,
                   dp.updated_at dp_updated_at,
                   r.record_id latest_record_id,r.doctor_id latest_doctor_id,
                   r.visit_date latest_visit_date,r.final_diagnosis latest_final_diagnosis,
                   r.status latest_record_status,
                   l.log_id today_log_id,l.log_date today_log_date,
                   l.blood_glucose today_blood_glucose,l.systolic_bp today_systolic_bp,
                   l.diastolic_bp today_diastolic_bp,l.weight today_weight,
                   l.symptoms today_symptoms,l.note today_note,
                   l.heart_rate today_heart_rate,l.spo2 today_spo2,
                   l.meal_type today_meal_type,l.created_at today_created_at
            FROM patients p
            LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
            OUTER APPLY (
              SELECT TOP 1 record_id,doctor_id,visit_date,final_diagnosis,status
              FROM medicalrecords r0 WHERE r0.patient_id=p.patient_id
              ORDER BY visit_date DESC
            ) r
            LEFT JOIN patientdailylogs l
              ON l.patient_id=p.patient_id AND l.log_date=CAST(SYSDATETIME() AS DATE)
            WHERE p.user_id=?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rows = ps.executeQuery()) {
                Patient patient = null;
                DiabetesProfile diabetesProfile = null;
                MedicalRecord latestRecord = null;
                PatientDailyLog todayLog = null;
                while (rows.next()) {
                    if (patient == null) {
                        patient = mapRow(rows);
                        diabetesProfile = new DiabetesProfile();
                        diabetesProfile.setPatientId(patient.getPatientId());
                        diabetesProfile.setDiabetesType(rows.getString("dp_type"));
                        Date diagnosisDate = rows.getDate("dp_diagnosis_date");
                        if (diagnosisDate != null) diabetesProfile.setDiagnosisDate(diagnosisDate.toLocalDate());
                        diabetesProfile.setTreatmentMethod(rows.getString("dp_treatment_method"));
                        setNullableDouble(rows, "dp_hba1c_target", diabetesProfile::setHba1cTarget);
                        Timestamp profileUpdated = rows.getTimestamp("dp_updated_at");
                        if (profileUpdated != null) diabetesProfile.setUpdatedAt(profileUpdated.toLocalDateTime());
                        if (rows.getObject("latest_record_id") != null) {
                            latestRecord = new MedicalRecord();
                            latestRecord.setRecordId(rows.getInt("latest_record_id"));
                            latestRecord.setPatientId(patient.getPatientId());
                            latestRecord.setDoctorId(rows.getInt("latest_doctor_id"));
                            Timestamp visit = rows.getTimestamp("latest_visit_date");
                            if (visit != null) latestRecord.setVisitDate(visit.toLocalDateTime());
                            latestRecord.setFinalDiagnosis(rows.getString("latest_final_diagnosis"));
                            latestRecord.setStatus(rows.getString("latest_record_status"));
                        }
                        if (rows.getObject("today_log_id") != null) {
                            todayLog = new PatientDailyLog();
                            todayLog.setLogId(rows.getInt("today_log_id"));
                            todayLog.setPatientId(patient.getPatientId());
                            Date logDate = rows.getDate("today_log_date");
                            if (logDate != null) todayLog.setLogDate(logDate.toLocalDate());
                            setNullableDouble(rows, "today_blood_glucose", todayLog::setBloodGlucose);
                            setNullableInteger(rows, "today_systolic_bp", todayLog::setSystolicBp);
                            setNullableInteger(rows, "today_diastolic_bp", todayLog::setDiastolicBp);
                            setNullableDouble(rows, "today_weight", todayLog::setWeight);
                            todayLog.setSymptoms(rows.getString("today_symptoms"));
                            todayLog.setNote(rows.getString("today_note"));
                            setNullableInteger(rows, "today_heart_rate", todayLog::setHeartRate);
                            setNullableDouble(rows, "today_spo2", todayLog::setSpo2);
                            todayLog.setMealType(rows.getString("today_meal_type"));
                            Timestamp created = rows.getTimestamp("today_created_at");
                            if (created != null) todayLog.setCreatedAt(created.toLocalDateTime());
                        }
                    }
                }
                return new PatientDashboardData(patient, diabetesProfile, latestRecord, todayLog);
            }
        } catch (SQLException error) {
            throw databaseError("load patient dashboard", error);
        }
    }

    private void setNullableDouble(ResultSet rows, String column,
            java.util.function.Consumer<Double> setter) throws SQLException {
        Object value = rows.getObject(column);
        if (value != null) setter.accept(((Number) value).doubleValue());
    }

    private void setNullableInteger(ResultSet rows, String column,
            java.util.function.Consumer<Integer> setter) throws SQLException {
        Object value = rows.getObject(column);
        if (value != null) setter.accept(((Number) value).intValue());
    }

    public record PatientDashboardData(Patient patient, DiabetesProfile diabetesProfile,
            MedicalRecord latestRecord, PatientDailyLog todayLog) {}

    public Patient create(Patient p) {
        String sql = "INSERT INTO Patients(user_id,full_name,date_of_birth,gender,phone,address,"
                   + "health_insurance_no,national_id,national_id_date,national_id_place,created_by)"
                   + " VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            if (p.getUserId() > 0) statement.setInt(1, p.getUserId());
            else statement.setNull(1, Types.INTEGER);
            statement.setString(2, p.getFullName());
            statement.setDate(3, p.getDateOfBirth() != null ? Date.valueOf(p.getDateOfBirth()) : null);
            statement.setString(4, p.getGender());
            statement.setString(5, p.getPhone());
            statement.setString(6, p.getAddress());
            statement.setString(7, p.getHealthInsuranceNo());
            statement.setString(8, p.getNationalId());
            statement.setDate(9, p.getNationalIdDate() != null ? Date.valueOf(p.getNationalIdDate()) : null);
            statement.setString(10, p.getNationalIdPlace());
            statement.setInt(11, p.getCreatedBy());
            if (statement.executeUpdate() > 0) {
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) p.setPatientId(keys.getInt(1));
                }
            }
        } catch (SQLException e) { throw databaseError("create patient", e); }
        return p;
    }

    public void update(Patient p) {
        String sql = "UPDATE Patients SET full_name=?,date_of_birth=?,gender=?,phone=?,address=?,"
                   + "health_insurance_no=?,national_id=?,national_id_date=?,national_id_place=?"
                   + " WHERE patient_id=?";
        boolean manageTransaction = false;
        try {
            manageTransaction = connection.getAutoCommit();
            if (manageTransaction) connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, p.getFullName());
                statement.setDate(2, p.getDateOfBirth() != null ? Date.valueOf(p.getDateOfBirth()) : null);
                statement.setString(3, p.getGender());
                statement.setString(4, p.getPhone());
                statement.setString(5, p.getAddress());
                statement.setString(6, p.getHealthInsuranceNo());
                statement.setString(7, p.getNationalId());
                statement.setDate(8, p.getNationalIdDate() != null ? Date.valueOf(p.getNationalIdDate()) : null);
                statement.setString(9, p.getNationalIdPlace());
                statement.setInt(10, p.getPatientId());
                if (statement.executeUpdate() != 1) {
                    throw new IllegalArgumentException("Không tìm thấy hồ sơ bệnh nhân để cập nhật.");
                }
            }
            if (p.getUserId() > 0) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE Users SET full_name=?,dob=?,gender=?,phone=?,address=? WHERE user_id=?")) {
                    statement.setString(1, p.getFullName());
                    statement.setDate(2, p.getDateOfBirth() != null ? Date.valueOf(p.getDateOfBirth()) : null);
                    statement.setString(3, p.getGender());
                    statement.setString(4, p.getPhone());
                    statement.setString(5, p.getAddress());
                    statement.setInt(6, p.getUserId());
                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException("Không tìm thấy tài khoản bệnh nhân để đồng bộ.");
                    }
                }
            }
            if (manageTransaction) connection.commit();
        } catch (RuntimeException | SQLException error) {
            if (manageTransaction) {
                try { connection.rollback(); }
                catch (SQLException rollbackError) { error.addSuppressed(rollbackError); }
            }
            throw error instanceof RuntimeException runtime
                    ? runtime : databaseError("update patient", (SQLException) error);
        } finally {
            if (manageTransaction) {
                try { connection.setAutoCommit(true); }
                catch (SQLException error) {
                    throw databaseError("restore database transaction", error);
                }
            }
        }
    }

    public void updateBasicProfile(Patient p) throws SQLException {
        String sql = "UPDATE Patients SET full_name=?,date_of_birth=?,gender=?,phone=?,address=?,national_id=? WHERE patient_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, p.getFullName());
            statement.setDate(2, p.getDateOfBirth() != null ? Date.valueOf(p.getDateOfBirth()) : null);
            statement.setString(3, p.getGender());
            statement.setString(4, p.getPhone());
            statement.setString(5, p.getAddress());
            statement.setString(6, p.getNationalId());
            statement.setInt(7, p.getPatientId());
            statement.executeUpdate();
        } catch (SQLException e) { throw e; }
    }

}
