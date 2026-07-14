package dal;

import models.HealthAlert;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * [NEW - Upgrade V3] DAO cho HealthAlerts — thay AIWarningDAO cho bệnh nhân.
 */
public class HealthAlertDAO extends DBContext {
    PreparedStatement stm;
    ResultSet rs;

    private HealthAlert mapRow(ResultSet rs) throws SQLException {
        HealthAlert a = new HealthAlert();
        a.setAlertId(rs.getInt("alert_id"));
        a.setPatientId(rs.getInt("patient_id"));
        a.setIndicatorType(rs.getString("indicator_type"));
        a.setValue(rs.getDouble("value"));
        a.setThreshold(rs.getDouble("threshold"));
        a.setAlertLevel(rs.getString("alert_level"));
        a.setAlertMessage(rs.getString("alert_message"));
        a.setDataSource(rs.getString("data_source"));
        int src = rs.getInt("source_record_id"); if (!rs.wasNull()) a.setSourceRecordId(src);
        a.setAcknowledged(rs.getBoolean("is_acknowledged"));
        Timestamp ack = rs.getTimestamp("acknowledged_at"); if (ack != null) a.setAcknowledgedAt(ack.toLocalDateTime());
        Timestamp ca  = rs.getTimestamp("created_at");      if (ca  != null) a.setCreatedAt(ca.toLocalDateTime());
        return a;
    }

    public HealthAlert save(HealthAlert alert) {
        String sql = "INSERT INTO HealthAlerts(patient_id,indicator_type,value,threshold," +
            "alert_level,alert_message,data_source,source_record_id) VALUES(?,?,?,?,?,?,?,?)";
        try {
            stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stm.setInt(1, alert.getPatientId());
            stm.setString(2, alert.getIndicatorType());
            stm.setDouble(3, alert.getValue());
            stm.setDouble(4, alert.getThreshold());
            stm.setString(5, alert.getAlertLevel());
            stm.setString(6, alert.getAlertMessage());
            stm.setString(7, alert.getDataSource() != null ? alert.getDataSource() : "manual");
            if (alert.getSourceRecordId() != null) stm.setInt(8, alert.getSourceRecordId());
            else stm.setNull(8, Types.INTEGER);
            stm.executeUpdate();
            rs = stm.getGeneratedKeys();
            if (rs.next()) alert.setAlertId(rs.getInt(1));
        } catch (SQLException e) { System.out.println("HealthAlertDAO.save: " + e.getMessage()); }
        return alert;
    }

    public List<HealthAlert> getUnacknowledged(int patientId) {
        List<HealthAlert> list = new ArrayList<>();
        String sql = "SELECT * FROM HealthAlerts WHERE patient_id=? AND is_acknowledged=FALSE " +
            "ORDER BY CASE alert_level WHEN 'high' THEN 1 WHEN 'medium' THEN 2 ELSE 3 END, created_at DESC";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, patientId);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("HealthAlertDAO.getUnacknowledged: " + e.getMessage()); }
        return list;
    }

    public List<HealthAlert> getRecent(int patientId, int limit) {
        List<HealthAlert> list = new ArrayList<>();
        try {
            stm = connection.prepareStatement(
                "SELECT * FROM HealthAlerts WHERE patient_id=? ORDER BY created_at DESC LIMIT ?");
            stm.setInt(1, patientId); stm.setInt(2, limit);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("HealthAlertDAO.getRecent: " + e.getMessage()); }
        return list;
    }

    public void acknowledge(int alertId) {
        try {
            stm = connection.prepareStatement(
                "UPDATE HealthAlerts SET is_acknowledged=TRUE,acknowledged_at=CURRENT_TIMESTAMP WHERE alert_id=?");
            stm.setInt(1, alertId);
            stm.executeUpdate();
        } catch (SQLException e) { System.out.println("HealthAlertDAO.acknowledge: " + e.getMessage()); }
    }

    public boolean acknowledgeForPatient(int alertId, int patientId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE HealthAlerts SET is_acknowledged=TRUE,acknowledged_at=CURRENT_TIMESTAMP WHERE alert_id=? AND patient_id=?")) {
            ps.setInt(1, alertId); ps.setInt(2, patientId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new IllegalStateException("Không thể xác nhận cảnh báo", e); }
    }

    public int countUnacknowledged(int patientId) {
        try {
            stm = connection.prepareStatement(
                "SELECT COUNT(*) FROM HealthAlerts WHERE patient_id=? AND is_acknowledged=FALSE");
            stm.setInt(1, patientId);
            rs = stm.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.out.println("HealthAlertDAO.countUnacknowledged: " + e.getMessage()); }
        return 0;
    }
}
