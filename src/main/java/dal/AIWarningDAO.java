package dal;

import models.AIWarning;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AIWarningDAO extends DBContext {
    PreparedStatement stm;
    ResultSet rs;

    private AIWarning mapRow(ResultSet rs) throws SQLException {
        AIWarning w = new AIWarning();
        w.setWarningId(rs.getInt("warning_id"));
        w.setRecordId(rs.getInt("record_id"));
        w.setRiskLevel(rs.getString("risk_level"));
        w.setWarningMessage(rs.getString("warning_message"));
        w.setSuggestedAction(rs.getString("suggested_action"));
        w.setAiScore(rs.getDouble("ai_score"));
        w.setReviewedByDoctor(rs.getBoolean("reviewed_by_doctor"));
        Timestamp t = rs.getTimestamp("generated_at");
        if (t != null) w.setGeneratedAt(t.toLocalDateTime());
        return w;
    }

    public AIWarning getByRecordId(int recordId) {
        String sql = "SELECT * FROM AIWarnings WHERE record_id=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, recordId);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.out.println("AIW.getByRecordId: " + e.getMessage()); }
        return null;
    }

    public AIWarning save(AIWarning w) {
        try {
            stm = connection.prepareStatement("DELETE FROM AIWarnings WHERE record_id=?");
            stm.setInt(1, w.getRecordId());
            stm.executeUpdate();

            String ins = "INSERT INTO AIWarnings(record_id,risk_level,warning_message,suggested_action,ai_score) "
                       + "VALUES(?,?,?,?,?)";
            stm = connection.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS);
            stm.setInt(1, w.getRecordId());
            stm.setString(2, w.getRiskLevel());
            stm.setString(3, w.getWarningMessage());
            stm.setString(4, w.getSuggestedAction());
            stm.setDouble(5, w.getAiScore());
            stm.executeUpdate();
            rs = stm.getGeneratedKeys();
            if (rs.next()) w.setWarningId(rs.getInt(1));
        } catch (SQLException e) { System.out.println("AIW.save: " + e.getMessage()); }
        return w;
    }

    public void markReviewed(int recordId) {
        try {
            stm = connection.prepareStatement(
                "UPDATE AIWarnings SET reviewed_by_doctor=TRUE WHERE record_id=?");
            stm.setInt(1, recordId);
            stm.executeUpdate();
        } catch (SQLException e) { System.out.println("AIW.markReviewed: " + e.getMessage()); }
    }

    public List<AIWarning> getRecentHigh(int limit) {
        List<AIWarning> list = new ArrayList<>();
        String sql = "SELECT * FROM AIWarnings WHERE risk_level IN ('HIGH','MEDIUM') "
                   + "ORDER BY generated_at DESC LIMIT ?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setInt(1, limit);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("AIW.getRecentHigh: " + e.getMessage()); }
        return list;
    }

    public int countByRisk(String riskLevel) {
        String sql = "SELECT COUNT(*) FROM AIWarnings WHERE risk_level=?";
        try {
            stm = connection.prepareStatement(sql);
            stm.setString(1, riskLevel);
            rs = stm.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.out.println("AIW.countByRisk: " + e.getMessage()); }
        return 0;
    }
}
