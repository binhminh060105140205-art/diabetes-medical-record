package dal;

import models.Patient;
import models.HealthAlert;
import models.MedicalRecord;
import models.PatientDailyLog;
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
        p.setNationalId(rs.getString("national_id"));
        Date ndate = rs.getDate("national_id_date");
        if (ndate != null) p.setNationalIdDate(ndate.toLocalDate());
        p.setNationalIdPlace(rs.getString("national_id_place"));
        p.setCreatedBy(rs.getInt("created_by"));
        return p;
    }

    public List<Patient> getAll() {
        List<Patient> list = new ArrayList<>();
        try {
            stm = connection.prepareStatement("SELECT * FROM Patients ORDER BY created_at DESC");
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { throw databaseError("load patients", e); }
        return list;
    }

    /** Loads total patient count and the dashboard list in one database round-trip. */
    public StaffDashboardData loadStaffDashboard(String keyword, int limit) {
        boolean searching = keyword != null && !keyword.isBlank();
        String where = searching
                ? " WHERE full_name ILIKE ? OR phone ILIKE ? OR health_insurance_no ILIKE ? OR national_id ILIKE ?"
                : "";
        String order = searching ? " ORDER BY full_name" : " ORDER BY created_at DESC";
        String sql = "WITH total AS (SELECT COUNT(*) value FROM patients), data AS (SELECT * FROM patients"
                + where + order + " LIMIT ?) SELECT d.*,t.value total FROM total t LEFT JOIN data d ON TRUE";
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

    /** Loads a filtered count and one page in one Aiven round-trip. */
    public PatientListData loadPatientList(String keyword, int page, int pageSize) {
        boolean searching = keyword != null && !keyword.isBlank();
        String where = searching
                ? " WHERE full_name ILIKE ? OR phone ILIKE ? OR health_insurance_no ILIKE ? OR national_id ILIKE ?"
                : "";
        String order = searching ? " ORDER BY full_name" : " ORDER BY created_at DESC";
        String sql = "WITH filtered AS (SELECT * FROM patients" + where
                + "), total AS (SELECT COUNT(*) value FROM filtered), data AS (SELECT * FROM filtered"
                + order + " LIMIT ? OFFSET ?) SELECT d.*,t.value total FROM total t LEFT JOIN data d ON TRUE";
        List<Patient> patients = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            if (searching) {
                String value = "%" + keyword.trim() + "%";
                for (int i = 0; i < 4; i++) ps.setString(index++, value);
            }
            ps.setInt(index++, pageSize);
            ps.setInt(index, (page - 1) * pageSize);
            try (ResultSet rows = ps.executeQuery()) {
                int total = 0;
                while (rows.next()) {
                    total = rows.getInt("total");
                    if (rows.getObject("patient_id") != null) patients.add(mapRow(rows));
                }
                return new PatientListData(total, patients);
            }
        } catch (SQLException error) { throw databaseError("load patient list", error); }
    }

    public record PatientListData(int total, List<Patient> patients) {}

    public Patient getById(int id) {
        try {
            stm = connection.prepareStatement("SELECT * FROM Patients WHERE patient_id=?");
            stm.setInt(1, id);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { throw databaseError("load patient", e); }
        return null;
    }

    public Patient getByUserId(int userId) {
        try {
            stm = connection.prepareStatement("SELECT * FROM Patients WHERE user_id=?");
            stm.setInt(1, userId);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { throw databaseError("load patient account", e); }
        return null;
    }

    /** Loads every read-only section of the patient home page in one Aiven round-trip. */
    public PatientDashboardData loadPatientDashboard(int userId) {
        String sql = """
            SELECT p.*,
                   r.record_id latest_record_id,r.doctor_id latest_doctor_id,
                   r.visit_date latest_visit_date,r.final_diagnosis latest_final_diagnosis,
                   r.status latest_record_status,
                   l.log_id today_log_id,l.log_date today_log_date,
                   l.blood_glucose today_blood_glucose,l.systolic_bp today_systolic_bp,
                   l.diastolic_bp today_diastolic_bp,l.weight today_weight,
                   l.symptoms today_symptoms,l.note today_note,
                   l.heart_rate today_heart_rate,l.spo2 today_spo2,
                   l.meal_type today_meal_type,l.created_at today_created_at,
                   a.alert_id alert_id,a.indicator_type alert_indicator_type,
                   a.value alert_value,a.threshold alert_threshold,
                   a.alert_level alert_level,a.alert_message alert_message,
                   a.data_source alert_data_source,a.source_record_id alert_source_record_id,
                   a.is_acknowledged alert_acknowledged,
                   a.acknowledged_at alert_acknowledged_at,a.created_at alert_created_at
            FROM patients p
            LEFT JOIN LATERAL (
              SELECT record_id,doctor_id,visit_date,final_diagnosis,status
              FROM medicalrecords WHERE patient_id=p.patient_id
              ORDER BY visit_date DESC LIMIT 1
            ) r ON TRUE
            LEFT JOIN patientdailylogs l
              ON l.patient_id=p.patient_id AND l.log_date=CURRENT_DATE
            LEFT JOIN healthalerts a
              ON a.patient_id=p.patient_id AND a.is_acknowledged=FALSE
            WHERE p.user_id=?
            ORDER BY CASE a.alert_level WHEN 'high' THEN 1 WHEN 'medium' THEN 2 ELSE 3 END,
                     a.created_at DESC
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rows = ps.executeQuery()) {
                Patient patient = null;
                MedicalRecord latestRecord = null;
                PatientDailyLog todayLog = null;
                List<HealthAlert> alerts = new ArrayList<>();
                while (rows.next()) {
                    if (patient == null) {
                        patient = mapRow(rows);
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
                    if (rows.getObject("alert_id") != null) alerts.add(mapDashboardAlert(rows, patient.getPatientId()));
                }
                return new PatientDashboardData(patient, latestRecord, todayLog, alerts);
            }
        } catch (SQLException error) {
            throw databaseError("load patient dashboard", error);
        }
    }

    private HealthAlert mapDashboardAlert(ResultSet rows, int patientId) throws SQLException {
        HealthAlert alert = new HealthAlert();
        alert.setAlertId(rows.getInt("alert_id"));
        alert.setPatientId(patientId);
        alert.setIndicatorType(rows.getString("alert_indicator_type"));
        alert.setValue(rows.getDouble("alert_value"));
        alert.setThreshold(rows.getDouble("alert_threshold"));
        alert.setAlertLevel(rows.getString("alert_level"));
        alert.setAlertMessage(rows.getString("alert_message"));
        alert.setDataSource(rows.getString("alert_data_source"));
        Object source = rows.getObject("alert_source_record_id");
        if (source != null) alert.setSourceRecordId(((Number) source).intValue());
        alert.setAcknowledged(rows.getBoolean("alert_acknowledged"));
        Timestamp acknowledged = rows.getTimestamp("alert_acknowledged_at");
        if (acknowledged != null) alert.setAcknowledgedAt(acknowledged.toLocalDateTime());
        Timestamp created = rows.getTimestamp("alert_created_at");
        if (created != null) alert.setCreatedAt(created.toLocalDateTime());
        return alert;
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

    public record PatientDashboardData(Patient patient, MedicalRecord latestRecord,
            PatientDailyLog todayLog, List<HealthAlert> alerts) {}

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
        } catch (SQLException e) { throw databaseError("create patient", e); }
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
        } catch (SQLException e) { throw databaseError("update patient", e); }
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
        } catch (SQLException e) { throw e; }
    }

}
