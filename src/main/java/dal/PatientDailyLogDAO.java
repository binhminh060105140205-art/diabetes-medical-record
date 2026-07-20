package dal;

import models.PatientDailyLog;
import models.Patient;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Stores and loads each patient's daily self-monitoring data. */
public class PatientDailyLogDAO extends DBContext {
    private PatientDailyLog mapRow(ResultSet rs) throws SQLException {
        PatientDailyLog l = new PatientDailyLog();
        l.setLogId(rs.getInt("log_id"));
        l.setPatientId(rs.getInt("patient_id"));
        Date d = rs.getDate("log_date");             if (d != null) l.setLogDate(d.toLocalDate());
        double bg = rs.getDouble("blood_glucose");   if (!rs.wasNull()) l.setBloodGlucose(bg);
        int sbp   = rs.getInt("systolic_bp");        if (!rs.wasNull()) l.setSystolicBp(sbp);
        int dbp   = rs.getInt("diastolic_bp");       if (!rs.wasNull()) l.setDiastolicBp(dbp);
        double w  = rs.getDouble("weight");          if (!rs.wasNull()) l.setWeight(w);
        l.setSymptoms(rs.getString("symptoms"));
        l.setNote(rs.getString("note"));
        Timestamp t = rs.getTimestamp("created_at"); if (t != null) l.setCreatedAt(t.toLocalDateTime());
        int hr = rs.getInt("heart_rate");        if (!rs.wasNull()) l.setHeartRate(hr);
        double spo2 = rs.getDouble("spo2");      if (!rs.wasNull()) l.setSpo2(spo2);
        l.setMealType(rs.getString("meal_type"));
        return l;
    }

    public PatientDailyLog save(PatientDailyLog log) {
        String sql = """
            INSERT INTO patientdailylogs(patient_id,log_date,blood_glucose,systolic_bp,diastolic_bp,
              weight,symptoms,note,heart_rate,spo2,meal_type)
            VALUES(?,CURRENT_DATE,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (patient_id,log_date) DO UPDATE SET
              blood_glucose=EXCLUDED.blood_glucose,systolic_bp=EXCLUDED.systolic_bp,
              diastolic_bp=EXCLUDED.diastolic_bp,weight=EXCLUDED.weight,
              symptoms=EXCLUDED.symptoms,note=EXCLUDED.note,heart_rate=EXCLUDED.heart_rate,
              spo2=EXCLUDED.spo2,meal_type=EXCLUDED.meal_type
            RETURNING log_id
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, log.getPatientId());
            setND(ps,2,log.getBloodGlucose()); setNI(ps,3,log.getSystolicBp());
            setNI(ps,4,log.getDiastolicBp());  setND(ps,5,log.getWeight());
            ps.setString(6, log.getSymptoms()); ps.setString(7, log.getNote());
            setNI(ps,8,log.getHeartRate());    setND(ps,9,log.getSpo2());
            ps.setString(10, log.getMealType());
            try (ResultSet rows = ps.executeQuery()) {
                if (rows.next()) log.setLogId(rows.getInt(1));
            }
        } catch (SQLException e) { throw databaseError("save patient daily log", e); }
        return log;
    }

    /** Resolves the patient account and loads its journal in one database round-trip. */
    public PatientJournalData loadJournalForUser(int userId, int limit) {
        String sql = """
            WITH subject AS (SELECT patient_id FROM patients WHERE user_id=?)
            SELECT s.patient_id subject_patient_id,l.*
            FROM subject s LEFT JOIN LATERAL (
              SELECT * FROM patientdailylogs WHERE patient_id=s.patient_id
              ORDER BY log_date DESC LIMIT ?
            ) l ON TRUE
            """;
        List<PatientDailyLog> logs = new ArrayList<>();
        Integer patientId = null;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, limit);
            try (ResultSet rows = ps.executeQuery()) {
                while (rows.next()) {
                    patientId = rows.getInt("subject_patient_id");
                    if (rows.getObject("log_id") != null) logs.add(mapRow(rows));
                }
            }
        } catch (SQLException error) {
            throw databaseError("load patient journal", error);
        }
        return new PatientJournalData(patientId, logs);
    }

    public record PatientJournalData(Integer patientId, List<PatientDailyLog> logs) {}

    /** Checks doctor access and loads the patient journal in one database round-trip. */
    public DoctorJournalData loadJournalForDoctor(int doctorUserId, int patientId, int limit) {
        String sql = """
                WITH subject AS (
                  SELECT p.patient_id,p.full_name,EXISTS (
                    SELECT 1 FROM doctors d JOIN encounters e ON e.doctor_id=d.doctor_id
                    WHERE d.user_id=? AND e.patient_id=p.patient_id
                  ) authorized
                  FROM patients p WHERE p.patient_id=?
                )
                SELECT s.patient_id subject_patient_id,s.full_name subject_full_name,
                       s.authorized,l.*
                FROM subject s LEFT JOIN LATERAL (
                  SELECT * FROM patientdailylogs WHERE patient_id=s.patient_id
                  ORDER BY log_date DESC LIMIT ?
                ) l ON TRUE
                """;
        Patient patient = null;
        boolean authorized = false;
        List<PatientDailyLog> logs = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, doctorUserId);
            statement.setInt(2, patientId);
            statement.setInt(3, limit);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    if (patient == null) {
                        patient = new Patient();
                        patient.setPatientId(rows.getInt("subject_patient_id"));
                        patient.setFullName(rows.getString("subject_full_name"));
                        authorized = rows.getBoolean("authorized");
                    }
                    if (rows.getObject("log_id") != null) logs.add(mapRow(rows));
                }
            }
        } catch (SQLException error) {
            throw databaseError("load doctor patient journal", error);
        }
        return new DoctorJournalData(patient, authorized, logs);
    }

    public record DoctorJournalData(Patient patient, boolean authorized,
            List<PatientDailyLog> logs) {}

    private void setND(PreparedStatement s, int i, Double v) throws SQLException {
        if (v != null && v > 0) s.setDouble(i, v); else s.setNull(i, Types.DOUBLE);
    }
    private void setNI(PreparedStatement s, int i, Integer v) throws SQLException {
        if (v != null && v > 0) s.setInt(i, v); else s.setNull(i, Types.INTEGER);
    }
}
