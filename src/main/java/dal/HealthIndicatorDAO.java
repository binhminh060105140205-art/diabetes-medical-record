package dal;

import models.HealthIndicator;
import java.sql.*;

public class HealthIndicatorDAO extends DBContext {
    /** Saves triage-only fields and returns the linked encounter without another query. */
    public Integer saveClinical(HealthIndicator indicator) {
        String sql = """
                WITH saved AS (
                  INSERT INTO healthindicators(
                    record_id,entered_by_staff,height,weight,bmi,systolic_bp,
                    diastolic_bp,heart_rate,temperature)
                  VALUES(?,?,?,?,?,?,?,?,?)
                  ON CONFLICT (record_id) DO UPDATE SET
                    entered_by_staff=EXCLUDED.entered_by_staff,
                    height=EXCLUDED.height,weight=EXCLUDED.weight,bmi=EXCLUDED.bmi,
                    systolic_bp=EXCLUDED.systolic_bp,diastolic_bp=EXCLUDED.diastolic_bp,
                    heart_rate=EXCLUDED.heart_rate,temperature=EXCLUDED.temperature,
                    measured_at=CURRENT_TIMESTAMP
                  RETURNING indicator_id,record_id
                )
                SELECT saved.indicator_id,medicalrecords.encounter_id
                FROM saved JOIN medicalrecords USING(record_id)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, indicator.getRecordId());
            statement.setInt(2, indicator.getEnteredByStaff());
            statement.setDouble(3, indicator.getHeight());
            statement.setDouble(4, indicator.getWeight());
            statement.setDouble(5, indicator.getBmi());
            statement.setInt(6, indicator.getSystolicBp());
            statement.setInt(7, indicator.getDiastolicBp());
            statement.setInt(8, indicator.getHeartRate());
            statement.setDouble(9, indicator.getTemperature());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("Clinical indicator was not saved");
                indicator.setIndicatorId(rows.getInt("indicator_id"));
                Object encounterId = rows.getObject("encounter_id");
                return encounterId == null ? null : ((Number) encounterId).intValue();
            }
        } catch (SQLException error) {
            throw databaseError("save clinical indicators", error);
        }
    }

    /** Saves laboratory values without replacing the triage measurements. Blank values stay NULL. */
    public void saveLabResults(int recordId, int staffId, Double bloodGlucose, Double hba1c,
            Double cholesterol, Double triglyceride, Double hdlC, Double ldlC) {
        String sql = """
                INSERT INTO healthindicators(
                    record_id,entered_by_staff,blood_glucose,hba1c,cholesterol,triglyceride,hdl_c,ldl_c)
                VALUES(?,?,?,?,?,?,?,?)
                ON CONFLICT (record_id) DO UPDATE SET
                    entered_by_staff=EXCLUDED.entered_by_staff,
                    blood_glucose=COALESCE(EXCLUDED.blood_glucose,healthindicators.blood_glucose),
                    hba1c=COALESCE(EXCLUDED.hba1c,healthindicators.hba1c),
                    cholesterol=COALESCE(EXCLUDED.cholesterol,healthindicators.cholesterol),
                    triglyceride=COALESCE(EXCLUDED.triglyceride,healthindicators.triglyceride),
                    hdl_c=COALESCE(EXCLUDED.hdl_c,healthindicators.hdl_c),
                    ldl_c=COALESCE(EXCLUDED.ldl_c,healthindicators.ldl_c),
                    measured_at=CURRENT_TIMESTAMP""";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, recordId);
            statement.setInt(2, staffId);
            setNullable(statement, 3, bloodGlucose);
            setNullable(statement, 4, hba1c);
            setNullable(statement, 5, cholesterol);
            setNullable(statement, 6, triglyceride);
            setNullable(statement, 7, hdlC);
            setNullable(statement, 8, ldlC);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Laboratory indicators were not saved");
            }
        } catch (SQLException error) {
            throw databaseError("save laboratory indicators", error);
        }
    }

    private void setNullable(PreparedStatement statement, int index, Double value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.NUMERIC);
        else statement.setDouble(index, value);
    }

}
