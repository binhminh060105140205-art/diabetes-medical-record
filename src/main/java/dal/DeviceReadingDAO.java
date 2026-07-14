package dal;

import models.DeviceReading;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * [NEW - Upgrade V3] DAO cho DeviceReadings.
 */
public class DeviceReadingDAO extends DBContext {
    PreparedStatement stm;
    ResultSet rs;

    private DeviceReading mapRow(ResultSet rs) throws SQLException {
        DeviceReading d = new DeviceReading();
        d.setId(rs.getInt("id"));
        d.setPatientId(rs.getInt("patient_id"));
        d.setDeviceType(rs.getString("device_type"));
        d.setRawJsonData(rs.getString("raw_json_data"));
        double g = rs.getDouble("parsed_glucose");      if (!rs.wasNull()) d.setParsedGlucose(g);
        int hr  = rs.getInt("parsed_heart_rate");       if (!rs.wasNull()) d.setParsedHeartRate(hr);
        int sbp = rs.getInt("parsed_systolic_bp");      if (!rs.wasNull()) d.setParsedSystolicBp(sbp);
        int dbp = rs.getInt("parsed_diastolic_bp");     if (!rs.wasNull()) d.setParsedDiastolicBp(dbp);
        double w = rs.getDouble("parsed_weight");       if (!rs.wasNull()) d.setParsedWeight(w);
        double s = rs.getDouble("parsed_spo2");         if (!rs.wasNull()) d.setParsedSpo2(s);
        Timestamp ma = rs.getTimestamp("measured_at");  if (ma != null) d.setMeasuredAt(ma.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("created_at");   if (ca != null) d.setCreatedAt(ca.toLocalDateTime());
        d.setAbnormal(rs.getBoolean("is_abnormal"));
        d.setAbnormalNote(rs.getString("abnormal_note"));
        return d;
    }

    public DeviceReading save(DeviceReading dr) {
        String sql = "INSERT INTO DeviceReadings(patient_id,device_type,raw_json_data," +
            "parsed_glucose,parsed_heart_rate,parsed_systolic_bp,parsed_diastolic_bp," +
            "parsed_weight,parsed_spo2,measured_at,is_abnormal,abnormal_note) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stm.setInt(1, dr.getPatientId());
            stm.setString(2, dr.getDeviceType());
            stm.setString(3, dr.getRawJsonData());
            setND(stm, 4, dr.getParsedGlucose());
            setNI(stm, 5, dr.getParsedHeartRate());
            setNI(stm, 6, dr.getParsedSystolicBp());
            setNI(stm, 7, dr.getParsedDiastolicBp());
            setND(stm, 8, dr.getParsedWeight());
            setND(stm, 9, dr.getParsedSpo2());
            if (dr.getMeasuredAt() != null) stm.setTimestamp(10, Timestamp.valueOf(dr.getMeasuredAt()));
            else stm.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
            stm.setBoolean(11, dr.isAbnormal());
            stm.setString(12, dr.getAbnormalNote());
            stm.executeUpdate();
            rs = stm.getGeneratedKeys();
            if (rs.next()) dr.setId(rs.getInt(1));
        } catch (SQLException e) { System.out.println("DeviceReadingDAO.save: " + e.getMessage()); }
        return dr;
    }

    public List<DeviceReading> getRecent(int patientId, int limit) {
        List<DeviceReading> list = new ArrayList<>();
        try {
            stm = connection.prepareStatement(
                "SELECT * FROM DeviceReadings WHERE patient_id=? ORDER BY measured_at DESC LIMIT ?");
            stm.setInt(1, patientId); stm.setInt(2, limit);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("DeviceReadingDAO.getRecent: " + e.getMessage()); }
        return list;
    }

    public List<DeviceReading> getAbnormal(int patientId) {
        List<DeviceReading> list = new ArrayList<>();
        try {
            stm = connection.prepareStatement(
                "SELECT * FROM DeviceReadings WHERE patient_id=? AND is_abnormal=TRUE ORDER BY measured_at DESC");
            stm.setInt(1, patientId);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("DeviceReadingDAO.getAbnormal: " + e.getMessage()); }
        return list;
    }

    public double getAvgGlucose7Days(int patientId) {
        try {
            stm = connection.prepareStatement(
                "SELECT AVG(parsed_glucose) FROM DeviceReadings WHERE patient_id=? " +
                "AND parsed_glucose IS NOT NULL AND measured_at >= CURRENT_TIMESTAMP - INTERVAL '7 days'");
            stm.setInt(1, patientId);
            rs = stm.executeQuery();
            if (rs.next()) { double v = rs.getDouble(1); if (!rs.wasNull()) return v; }
        } catch (SQLException e) { System.out.println("DeviceReadingDAO.getAvgGlucose7Days: " + e.getMessage()); }
        return 0.0;
    }

    private void setND(PreparedStatement s, int i, Double v) throws SQLException {
        if (v != null) s.setDouble(i, v); else s.setNull(i, Types.DOUBLE);
    }
    private void setNI(PreparedStatement s, int i, Integer v) throws SQLException {
        if (v != null) s.setInt(i, v); else s.setNull(i, Types.INTEGER);
    }
}
