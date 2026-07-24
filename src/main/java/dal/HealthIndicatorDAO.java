package dal;

import models.HealthIndicator;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class HealthIndicatorDAO extends DBContext {
    /** Saves triage-only fields and returns the linked encounter without another request. */
    public Integer saveClinical(HealthIndicator indicator) {
        try {
            int updated;
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE healthindicators SET
                      entered_by_staff=?,height=?,weight=?,bmi=?,systolic_bp=?,
                      diastolic_bp=?,heart_rate=?,temperature=?,measured_at=CURRENT_TIMESTAMP
                    WHERE record_id=?
                    """)) {
                bindClinical(statement, indicator, false);
                updated = statement.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO healthindicators(
                          record_id,entered_by_staff,height,weight,bmi,systolic_bp,
                          diastolic_bp,heart_rate,temperature)
                        VALUES(?,?,?,?,?,?,?,?,?)
                        """)) {
                    bindClinical(statement, indicator, true);
                    statement.executeUpdate();
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT h.indicator_id,m.encounter_id
                    FROM healthindicators h
                    JOIN medicalrecords m ON m.record_id=h.record_id
                    WHERE h.record_id=?
                    """)) {
                statement.setInt(1, indicator.getRecordId());
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) throw new SQLException("Clinical indicator was not saved");
                    indicator.setIndicatorId(rows.getInt("indicator_id"));
                    Object encounterId = rows.getObject("encounter_id");
                    return encounterId == null ? null : ((Number) encounterId).intValue();
                }
            }
        } catch (SQLException error) {
            throw databaseError("save clinical indicators", error);
        }
    }

    /** Saves laboratory values without replacing the triage measurements. Blank values stay NULL. */
    public void saveLabResults(int recordId, int staffId, Double bloodGlucose, Double hba1c,
            Double cholesterol, Double triglyceride, Double hdlC, Double ldlC) {
        try {
            int updated;
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE healthindicators SET
                      entered_by_staff=?,
                      blood_glucose=COALESCE(?,blood_glucose),
                      hba1c=COALESCE(?,hba1c),
                      cholesterol=COALESCE(?,cholesterol),
                      triglyceride=COALESCE(?,triglyceride),
                      hdl_c=COALESCE(?,hdl_c),
                      ldl_c=COALESCE(?,ldl_c),
                      measured_at=CURRENT_TIMESTAMP
                    WHERE record_id=?
                    """)) {
                statement.setInt(1, staffId);
                setNullable(statement, 2, bloodGlucose);
                setNullable(statement, 3, hba1c);
                setNullable(statement, 4, cholesterol);
                setNullable(statement, 5, triglyceride);
                setNullable(statement, 6, hdlC);
                setNullable(statement, 7, ldlC);
                statement.setInt(8, recordId);
                updated = statement.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO healthindicators(
                          record_id,entered_by_staff,blood_glucose,hba1c,cholesterol,triglyceride,hdl_c,ldl_c)
                        VALUES(?,?,?,?,?,?,?,?)
                        """)) {
                    statement.setInt(1, recordId);
                    statement.setInt(2, staffId);
                    setNullable(statement, 3, bloodGlucose);
                    setNullable(statement, 4, hba1c);
                    setNullable(statement, 5, cholesterol);
                    setNullable(statement, 6, triglyceride);
                    setNullable(statement, 7, hdlC);
                    setNullable(statement, 8, ldlC);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException error) {
            throw databaseError("save laboratory indicators", error);
        }
    }

    private void bindClinical(PreparedStatement statement, HealthIndicator indicator,
            boolean insert) throws SQLException {
        int index = 1;
        if (insert) statement.setInt(index++, indicator.getRecordId());
        statement.setInt(index++, indicator.getEnteredByStaff());
        statement.setDouble(index++, indicator.getHeight());
        statement.setDouble(index++, indicator.getWeight());
        statement.setDouble(index++, indicator.getBmi());
        statement.setInt(index++, indicator.getSystolicBp());
        statement.setInt(index++, indicator.getDiastolicBp());
        statement.setInt(index++, indicator.getHeartRate());
        statement.setDouble(index++, indicator.getTemperature());
        if (!insert) statement.setInt(index, indicator.getRecordId());
    }

    private void setNullable(PreparedStatement statement, int index, Double value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.NUMERIC);
        else statement.setDouble(index, value);
    }
}