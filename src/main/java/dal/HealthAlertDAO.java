package dal;

import models.HealthAlert;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HealthAlertDAO extends DBContext {
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
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, alert.getPatientId());
            statement.setString(2, alert.getIndicatorType());
            statement.setDouble(3, alert.getValue());
            statement.setDouble(4, alert.getThreshold());
            statement.setString(5, alert.getAlertLevel());
            statement.setString(6, alert.getAlertMessage());
            statement.setString(7, alert.getDataSource() != null ? alert.getDataSource() : "manual");
            if (alert.getSourceRecordId() != null) statement.setInt(8, alert.getSourceRecordId());
            else statement.setNull(8, Types.INTEGER);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) alert.setAlertId(keys.getInt(1));
            }
        } catch (SQLException e) { throw databaseError("save health alert", e); }
        return alert;
    }

    /** Returns recent alerts and the unread total with one indexed query. */
    public AlertOverview loadOverview(int patientId, int limit) {
        List<HealthAlert> alerts = new ArrayList<>();
        int unread = 0;
        String sql = """
                WITH unread AS (
                  SELECT COUNT(*) unread_total FROM healthalerts
                  WHERE patient_id=? AND NOT is_acknowledged
                ), recent AS (
                  SELECT * FROM healthalerts
                  WHERE patient_id=? ORDER BY created_at DESC LIMIT ?
                )
                SELECT recent.*,unread.unread_total
                FROM unread LEFT JOIN recent ON TRUE
                ORDER BY recent.created_at DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, patientId);
            statement.setInt(3, limit);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    unread = rows.getInt("unread_total");
                    if (rows.getObject("alert_id") != null) alerts.add(mapRow(rows));
                }
            }
        } catch (SQLException e) { throw databaseError("load recent health alerts", e); }
        return new AlertOverview(alerts, unread);
    }

    public boolean acknowledgeForPatient(int alertId, int patientId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE HealthAlerts SET is_acknowledged=TRUE,acknowledged_at=CURRENT_TIMESTAMP WHERE alert_id=? AND patient_id=?")) {
            ps.setInt(1, alertId); ps.setInt(2, patientId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new IllegalStateException("Không thể xác nhận cảnh báo", e); }
    }

    public record AlertOverview(List<HealthAlert> alerts, int unread) {}
}
