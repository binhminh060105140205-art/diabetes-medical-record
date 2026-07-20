package dal;

import models.DeviceReading;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeviceReadingDAO extends DBContext {
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
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, dr.getPatientId());
            statement.setString(2, dr.getDeviceType());
            statement.setString(3, dr.getRawJsonData());
            setND(statement, 4, dr.getParsedGlucose());
            setNI(statement, 5, dr.getParsedHeartRate());
            setNI(statement, 6, dr.getParsedSystolicBp());
            setNI(statement, 7, dr.getParsedDiastolicBp());
            setND(statement, 8, dr.getParsedWeight());
            setND(statement, 9, dr.getParsedSpo2());
            if (dr.getMeasuredAt() != null) {
                statement.setTimestamp(10, Timestamp.valueOf(dr.getMeasuredAt()));
            } else {
                statement.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
            }
            statement.setBoolean(11, dr.isAbnormal());
            statement.setString(12, dr.getAbnormalNote());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) dr.setId(keys.getInt(1));
            }
        } catch (SQLException e) { throw databaseError("save device reading", e); }
        return dr;
    }

    /** Resolves the patient account and recent readings in one database round-trip. */
    public DevicePageData loadPageForUser(int userId, int limit) {
        List<DeviceReading> readings = new ArrayList<>();
        String sql = """
                WITH subject AS (SELECT patient_id FROM patients WHERE user_id=?)
                SELECT s.patient_id subject_patient_id,d.*
                FROM subject s
                LEFT JOIN LATERAL (
                  SELECT * FROM devicereadings
                  WHERE patient_id=s.patient_id
                  ORDER BY measured_at DESC LIMIT ?
                ) d ON TRUE
                ORDER BY d.measured_at DESC
                """;
        Integer patientId = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, limit);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    patientId = rows.getInt("subject_patient_id");
                    if (rows.getObject("id") != null) readings.add(mapRow(rows));
                }
            }
        } catch (SQLException e) { throw databaseError("load device readings", e); }
        return new DevicePageData(patientId, readings);
    }

    public record DevicePageData(Integer patientId, List<DeviceReading> readings) {}

    private void setND(PreparedStatement s, int i, Double v) throws SQLException {
        if (v != null) s.setDouble(i, v); else s.setNull(i, Types.DOUBLE);
    }
    private void setNI(PreparedStatement s, int i, Integer v) throws SQLException {
        if (v != null) s.setInt(i, v); else s.setNull(i, Types.INTEGER);
    }
}
