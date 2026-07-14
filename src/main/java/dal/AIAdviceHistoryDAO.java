package dal;

import models.AIAdviceHistory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * [MODIFIED - Upgrade V3]
 * save() thêm: risk_level, recommendation_type, source_data_reference, avg_glucose_7d, avg_systolic_7d
 */
public class AIAdviceHistoryDAO extends DBContext {
    PreparedStatement stm;
    ResultSet rs;

    private AIAdviceHistory mapRow(ResultSet rs) throws SQLException {
        AIAdviceHistory a = new AIAdviceHistory();
        a.setAdviceId(rs.getInt("advice_id"));
        a.setPatientId(rs.getInt("patient_id"));
        Date d = rs.getDate("advice_date");
        if (d != null) a.setAdviceDate(d.toLocalDate());
        a.setAdviceContent(rs.getString("advice_content"));
        Timestamp t = rs.getTimestamp("created_at");
        if (t != null) a.setCreatedAt(t.toLocalDateTime());
        // [NEW V3] — dùng try-catch để tương thích DB cũ chưa migrate
        try {
            a.setRiskLevel(rs.getString("risk_level"));
            a.setRecommendationType(rs.getString("recommendation_type"));
            a.setSourceDataReference(rs.getString("source_data_reference"));
            double g = rs.getDouble("avg_glucose_7d"); if (!rs.wasNull()) a.setAvgGlucose7d(g);
            int sbp  = rs.getInt("avg_systolic_7d");  if (!rs.wasNull()) a.setAvgSystolic7d(sbp);
        } catch (SQLException ignored) {}
        return a;
    }

    /** [MODIFIED V3] INSERT thêm 5 trường mới */
    public AIAdviceHistory save(AIAdviceHistory a) {
        AIAdviceHistory today = getTodayAdvice(a.getPatientId());
        if (today != null) {
            // Update nội dung + các trường V3
            String sql = "UPDATE AIAdviceHistory SET advice_content=?,risk_level=?," +
                "recommendation_type=?,source_data_reference=?,avg_glucose_7d=?,avg_systolic_7d=? " +
                "WHERE advice_id=?";
            try {
                stm = connection.prepareStatement(sql);
                stm.setString(1, a.getAdviceContent());
                stm.setString(2, a.getRiskLevel());
                stm.setString(3, a.getRecommendationType());
                stm.setString(4, a.getSourceDataReference());
                if (a.getAvgGlucose7d() != null) stm.setDouble(5, a.getAvgGlucose7d()); else stm.setNull(5, Types.DOUBLE);
                if (a.getAvgSystolic7d() != null) stm.setInt(6, a.getAvgSystolic7d());   else stm.setNull(6, Types.INTEGER);
                stm.setInt(7, today.getAdviceId());
                stm.executeUpdate();
                a.setAdviceId(today.getAdviceId());
            } catch (SQLException e) { System.out.println("AIAdviceHistoryDAO.update: " + e.getMessage()); }
        } else {
            String sql = "INSERT INTO AIAdviceHistory(patient_id,advice_date,advice_content," +
                "risk_level,recommendation_type,source_data_reference,avg_glucose_7d,avg_systolic_7d) " +
                "VALUES(?,CURRENT_DATE,?,?,?,?,?,?)";
            try {
                stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stm.setInt(1, a.getPatientId());
                stm.setString(2, a.getAdviceContent());
                stm.setString(3, a.getRiskLevel());
                stm.setString(4, a.getRecommendationType());
                stm.setString(5, a.getSourceDataReference());
                if (a.getAvgGlucose7d() != null) stm.setDouble(6, a.getAvgGlucose7d()); else stm.setNull(6, Types.DOUBLE);
                if (a.getAvgSystolic7d() != null) stm.setInt(7, a.getAvgSystolic7d());   else stm.setNull(7, Types.INTEGER);
                stm.executeUpdate();
                rs = stm.getGeneratedKeys();
                if (rs.next()) a.setAdviceId(rs.getInt(1));
            } catch (SQLException e) { System.out.println("AIAdviceHistoryDAO.insert: " + e.getMessage()); }
        }
        return a;
    }

    public AIAdviceHistory getTodayAdvice(int patientId) {
        try {
            stm = connection.prepareStatement(
                "SELECT * FROM AIAdviceHistory WHERE patient_id=? AND advice_date=CURRENT_DATE");
            stm.setInt(1, patientId);
            rs = stm.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.out.println("AIAdviceHistoryDAO.getTodayAdvice: " + e.getMessage()); }
        return null;
    }

    public List<AIAdviceHistory> getRecent(int patientId, int limit) {
        List<AIAdviceHistory> list = new ArrayList<>();
        try {
            stm = connection.prepareStatement(
                "SELECT * FROM AIAdviceHistory WHERE patient_id=? ORDER BY advice_date DESC LIMIT ?");
            stm.setInt(1, patientId); stm.setInt(2, limit);
            rs = stm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println("AIAdviceHistoryDAO.getRecent: " + e.getMessage()); }
        return list;
    }
}
