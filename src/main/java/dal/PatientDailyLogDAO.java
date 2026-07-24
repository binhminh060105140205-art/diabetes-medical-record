package dal;

import models.PatientDailyLog;
import models.Patient;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Stores and loads each patient's daily self-monitoring data. */
public class PatientDailyLogDAO extends DBContext {
    private PatientDailyLog mapRow(ResultSet rows) throws SQLException {
        PatientDailyLog log = new PatientDailyLog();
        log.setLogId(rows.getInt("log_id"));
        log.setPatientId(rows.getInt("patient_id"));
        Date date = rows.getDate("log_date"); if (date != null) log.setLogDate(date.toLocalDate());
        double bloodGlucose = rows.getDouble("blood_glucose"); if (!rows.wasNull()) log.setBloodGlucose(bloodGlucose);
        int systolic = rows.getInt("systolic_bp"); if (!rows.wasNull()) log.setSystolicBp(systolic);
        int diastolic = rows.getInt("diastolic_bp"); if (!rows.wasNull()) log.setDiastolicBp(diastolic);
        double weight = rows.getDouble("weight"); if (!rows.wasNull()) log.setWeight(weight);
        log.setSymptoms(rows.getString("symptoms"));
        log.setNote(rows.getString("note"));
        Timestamp createdAt = rows.getTimestamp("created_at");
        if (createdAt != null) log.setCreatedAt(createdAt.toLocalDateTime());
        int heartRate = rows.getInt("heart_rate"); if (!rows.wasNull()) log.setHeartRate(heartRate);
        double spo2 = rows.getDouble("spo2"); if (!rows.wasNull()) log.setSpo2(spo2);
        log.setMealType(rows.getString("meal_type"));
        return log;
    }

    public PatientDailyLog save(PatientDailyLog log) {
        Date today = Date.valueOf(LocalDate.now());
        try {
            int updated;
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE patientdailylogs SET blood_glucose=?,systolic_bp=?,diastolic_bp=?,
                      weight=?,symptoms=?,note=?,heart_rate=?,spo2=?,meal_type=?
                    WHERE patient_id=? AND log_date=?
                    """)) {
                bindValues(statement, log, 1);
                statement.setInt(10, log.getPatientId());
                statement.setDate(11, today);
                updated = statement.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO patientdailylogs(patient_id,log_date,blood_glucose,systolic_bp,
                          diastolic_bp,weight,symptoms,note,heart_rate,spo2,meal_type)
                        VALUES(?,?,?,?,?,?,?,?,?,?,?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, log.getPatientId());
                    statement.setDate(2, today);
                    bindValues(statement, log, 3);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (keys.next()) log.setLogId(keys.getInt(1));
                    }
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT log_id FROM patientdailylogs WHERE patient_id=? AND log_date=?
                        """)) {
                    statement.setInt(1, log.getPatientId());
                    statement.setDate(2, today);
                    try (ResultSet rows = statement.executeQuery()) {
                        if (rows.next()) log.setLogId(rows.getInt(1));
                    }
                }
            }
        } catch (SQLException error) { throw databaseError("save patient daily log", error); }
        return log;
    }

    public PatientJournalData loadJournalForUser(int userId, int limit) {
        Integer patientId = null;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT patient_id FROM patients WHERE user_id=?")) {
            statement.setInt(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) patientId = rows.getInt(1);
            }
            return new PatientJournalData(patientId,
                    patientId == null ? List.of() : loadLogs(patientId, limit));
        } catch (SQLException error) {
            throw databaseError("load patient journal", error);
        }
    }

    public record PatientJournalData(Integer patientId, List<PatientDailyLog> logs) {}

    public DoctorJournalData loadJournalForDoctor(int doctorUserId, int patientId, int limit) {
        Patient patient = null;
        boolean authorized = false;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT p.patient_id,p.full_name,
                       CASE WHEN EXISTS (
                         SELECT 1 FROM doctors d JOIN encounters e ON e.doctor_id=d.doctor_id
                         WHERE d.user_id=? AND e.patient_id=p.patient_id
                       ) THEN 1 ELSE 0 END authorized
                FROM patients p WHERE p.patient_id=?
                """)) {
            statement.setInt(1, doctorUserId);
            statement.setInt(2, patientId);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    patient = new Patient();
                    patient.setPatientId(rows.getInt("patient_id"));
                    patient.setFullName(rows.getString("full_name"));
                    authorized = rows.getBoolean("authorized");
                }
            }
            List<PatientDailyLog> logs = patient == null ? List.of() : loadLogs(patientId, limit);
            return new DoctorJournalData(patient, authorized, logs);
        } catch (SQLException error) {
            throw databaseError("load doctor patient journal", error);
        }
    }

    public record DoctorJournalData(Patient patient, boolean authorized,
            List<PatientDailyLog> logs) {}

    private List<PatientDailyLog> loadLogs(int patientId, int limit) throws SQLException {
        List<PatientDailyLog> logs = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM patientdailylogs WHERE patient_id=? ORDER BY log_date DESC
                """)) {
            statement.setInt(1, patientId);
            statement.setMaxRows(Math.max(1, limit));
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) logs.add(mapRow(rows));
            }
        }
        return logs;
    }

    private void bindValues(PreparedStatement statement, PatientDailyLog log, int start)
            throws SQLException {
        setNullableDouble(statement, start, log.getBloodGlucose());
        setNullableInteger(statement, start + 1, log.getSystolicBp());
        setNullableInteger(statement, start + 2, log.getDiastolicBp());
        setNullableDouble(statement, start + 3, log.getWeight());
        statement.setString(start + 4, log.getSymptoms());
        statement.setString(start + 5, log.getNote());
        setNullableInteger(statement, start + 6, log.getHeartRate());
        setNullableDouble(statement, start + 7, log.getSpo2());
        statement.setString(start + 8, log.getMealType());
    }

    private void setNullableDouble(PreparedStatement statement, int index, Double value)
            throws SQLException {
        if (value != null && value > 0) statement.setDouble(index, value);
        else statement.setNull(index, Types.DOUBLE);
    }

    private void setNullableInteger(PreparedStatement statement, int index, Integer value)
            throws SQLException {
        if (value != null && value > 0) statement.setInt(index, value);
        else statement.setNull(index, Types.INTEGER);
    }
}