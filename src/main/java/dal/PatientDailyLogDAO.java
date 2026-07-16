package dal;

import models.PatientDailyLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * [MODIFIED - Upgrade V3]
 * Thêm xử lý: heart_rate, spo2, meal_type trong save/update/mapRow.
 * Lưu và đọc nhật ký sức khỏe hằng ngày.
 */
public class PatientDailyLogDAO extends DBContext {
    PreparedStatement stm;
    ResultSet rs;

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
        PatientDailyLog today = getTodayLog(log.getPatientId());
        if (today != null) {
            // [MODIFIED V3] thêm heart_rate, spo2, meal_type vào UPDATE
            String sql = "UPDATE PatientDailyLogs SET blood_glucose=?,systolic_bp=?,diastolic_bp=?," +
                "weight=?,symptoms=?,note=?,heart_rate=?,spo2=?,meal_type=? WHERE log_id=?";
            try {
                stm = connection.prepareStatement(sql);
                setND(stm,1,log.getBloodGlucose()); setNI(stm,2,log.getSystolicBp());
                setNI(stm,3,log.getDiastolicBp());  setND(stm,4,log.getWeight());
                stm.setString(5, log.getSymptoms()); stm.setString(6, log.getNote());
                setNI(stm,7,log.getHeartRate());    setND(stm,8,log.getSpo2());
                stm.setString(9, log.getMealType()); stm.setInt(10, today.getLogId());
                stm.executeUpdate();
                log.setLogId(today.getLogId());
            } catch (SQLException e) { throw databaseError("update patient daily log", e); }
        } else {
            // [MODIFIED V3] thêm heart_rate, spo2, meal_type vào INSERT
            String sql = "INSERT INTO PatientDailyLogs(patient_id,log_date,blood_glucose," +
                "systolic_bp,diastolic_bp,weight,symptoms,note,heart_rate,spo2,meal_type) " +
                "VALUES(?,CURRENT_DATE,?,?,?,?,?,?,?,?,?)";
            try {
                stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stm.setInt(1, log.getPatientId());
                setND(stm,2,log.getBloodGlucose()); setNI(stm,3,log.getSystolicBp());
                setNI(stm,4,log.getDiastolicBp());  setND(stm,5,log.getWeight());
                stm.setString(6, log.getSymptoms()); stm.setString(7, log.getNote());
                setNI(stm,8,log.getHeartRate());    setND(stm,9,log.getSpo2());
                stm.setString(10, log.getMealType());
                stm.executeUpdate();
                rs = stm.getGeneratedKeys();
                if (rs.next()) log.setLogId(rs.getInt(1));
            } catch (SQLException e) { throw databaseError("create patient daily log", e); }
        }
        return log;
    }

    public List<PatientDailyLog> getRecent(int patientId, int days) {
        List<PatientDailyLog> list = new ArrayList<>();
        try {
            stm = connection.prepareStatement(
                "SELECT * FROM PatientDailyLogs WHERE patient_id=? ORDER BY log_date DESC LIMIT ?");
            stm.setInt(1, patientId); stm.setInt(2, days);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { throw databaseError("load patient daily logs", e); }
        return list;
    }

    public PatientDailyLog getTodayLog(int patientId) {
        try {
            stm = connection.prepareStatement(
                "SELECT * FROM PatientDailyLogs WHERE patient_id=? AND log_date=CURRENT_DATE");
            stm.setInt(1, patientId);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { throw databaseError("load today's patient log", e); }
        return null;
    }

    private void setND(PreparedStatement s, int i, Double v) throws SQLException {
        if (v != null && v > 0) s.setDouble(i, v); else s.setNull(i, Types.DOUBLE);
    }
    private void setNI(PreparedStatement s, int i, Integer v) throws SQLException {
        if (v != null && v > 0) s.setInt(i, v); else s.setNull(i, Types.INTEGER);
    }
}
