package dal;

import models.HealthIndicator;
import java.sql.*;

public class HealthIndicatorDAO extends DBContext {
    PreparedStatement stm;
    ResultSet rs;

    private HealthIndicator mapRow(ResultSet rs) throws SQLException {
        HealthIndicator h = new HealthIndicator();
        h.setIndicatorId(rs.getInt("indicator_id"));
        h.setRecordId(rs.getInt("record_id"));
        h.setEnteredByStaff(rs.getInt("entered_by_staff"));
        h.setHeight(rs.getDouble("height"));
        h.setWeight(rs.getDouble("weight"));
        h.setBmi(rs.getDouble("bmi"));
        h.setSystolicBp(rs.getInt("systolic_bp"));
        h.setDiastolicBp(rs.getInt("diastolic_bp"));
        h.setHeartRate(rs.getInt("heart_rate"));
        h.setTemperature(rs.getDouble("temperature"));
        h.setBloodGlucose(rs.getDouble("blood_glucose"));
        h.setHba1c(rs.getDouble("hba1c"));
        h.setCholesterol(rs.getDouble("cholesterol"));
        h.setTriglyceride(rs.getDouble("triglyceride"));
        h.setHdlC(rs.getDouble("hdl_c"));
        h.setLdlC(rs.getDouble("ldl_c"));
        Timestamp t = rs.getTimestamp("measured_at");
        if (t != null) h.setMeasuredAt(t.toLocalDateTime());
        return h;
    }

    public HealthIndicator getByRecordId(int recordId) {
        String sql = "SELECT * FROM HealthIndicators WHERE record_id=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, recordId);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw databaseError("load health indicators", e);
        }
        return null;
    }

    public HealthIndicator save(HealthIndicator h) {
        // upsert pattern: insert or update
        HealthIndicator existing = getByRecordId(h.getRecordId());
        if (existing == null) {
            String sql = "INSERT INTO HealthIndicators(record_id,entered_by_staff,height,weight,bmi,"
                       + "systolic_bp,diastolic_bp,heart_rate,temperature,blood_glucose,hba1c,"
                       + "cholesterol,triglyceride,hdl_c,ldl_c) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
            try {
                stm = connection.prepareStatement(sql);
                stm.setInt(1, h.getRecordId());
                stm.setInt(2, h.getEnteredByStaff());
                stm.setDouble(3, h.getHeight());
                stm.setDouble(4, h.getWeight());
                stm.setDouble(5, h.getBmi());
                stm.setInt(6, h.getSystolicBp());
                stm.setInt(7, h.getDiastolicBp());
                stm.setInt(8, h.getHeartRate());
                stm.setDouble(9, h.getTemperature());
                stm.setDouble(10, h.getBloodGlucose());
                stm.setDouble(11, h.getHba1c());
                stm.setDouble(12, h.getCholesterol());
                stm.setDouble(13, h.getTriglyceride());
                stm.setDouble(14, h.getHdlC());
                stm.setDouble(15, h.getLdlC());
                stm.executeUpdate();
            } catch (SQLException e) {
                throw databaseError("create health indicators", e);
            }
        } else {
            String sql = "UPDATE HealthIndicators SET height=?,weight=?,bmi=?,systolic_bp=?,diastolic_bp=?,"
                       + "heart_rate=?,temperature=?,blood_glucose=?,hba1c=?,cholesterol=?,triglyceride=?,"
                       + "hdl_c=?,ldl_c=?,measured_at=CURRENT_TIMESTAMP WHERE record_id=?";
            try {
                stm = connection.prepareStatement(sql);
                stm.setDouble(1, h.getHeight());
                stm.setDouble(2, h.getWeight());
                stm.setDouble(3, h.getBmi());
                stm.setInt(4, h.getSystolicBp());
                stm.setInt(5, h.getDiastolicBp());
                stm.setInt(6, h.getHeartRate());
                stm.setDouble(7, h.getTemperature());
                stm.setDouble(8, h.getBloodGlucose());
                stm.setDouble(9, h.getHba1c());
                stm.setDouble(10, h.getCholesterol());
                stm.setDouble(11, h.getTriglyceride());
                stm.setDouble(12, h.getHdlC());
                stm.setDouble(13, h.getLdlC());
                stm.setInt(14, h.getRecordId());
                stm.executeUpdate();
            } catch (SQLException e) {
                throw databaseError("update health indicators", e);
            }
        }
        return h;
    }
}
