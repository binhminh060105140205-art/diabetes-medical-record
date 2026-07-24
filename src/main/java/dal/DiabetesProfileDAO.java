package dal;

import models.DiabetesProfile;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;

/** Reads and updates each patient's single diabetes profile (type, diagnosis, treatment). */
public class DiabetesProfileDAO extends DBContext {

    public DiabetesProfile getByPatientId(int patientId) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM diabetes_profiles WHERE patient_id=?")) {
            statement.setInt(1, patientId);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) return mapRow(rows);
            }
        } catch (SQLException error) { throw databaseError("load diabetes profile", error); }
        return null;
    }

    /** Creates the default UNKNOWN profile for a newly registered patient. Call within the caller's transaction. */
    public void createDefault(int patientId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO diabetes_profiles(patient_id)
                SELECT ? WHERE NOT EXISTS (
                  SELECT 1 FROM diabetes_profiles WHERE patient_id=?
                )
                """)) {
            statement.setInt(1, patientId);
            statement.setInt(2, patientId);
            statement.executeUpdate();
        }
    }

    /** Doctor-only update: confirms diabetes type and treatment plan for a patient. */
    public void update(int patientId, String diabetesType, LocalDate diagnosisDate,
                        String treatmentMethod, Double hba1cTarget) {
        try {
            int updated;
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE diabetes_profiles
                    SET diabetes_type=?,diagnosis_date=?,treatment_method=?,hba1c_target=?,
                        updated_at=CURRENT_TIMESTAMP
                    WHERE patient_id=?
                    """)) {
                bindProfile(statement, diabetesType, diagnosisDate, treatmentMethod, hba1cTarget);
                statement.setInt(5, patientId);
                updated = statement.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO diabetes_profiles(
                          patient_id,diabetes_type,diagnosis_date,treatment_method,hba1c_target)
                        VALUES(?,?,?,?,?)
                        """)) {
                    statement.setInt(1, patientId);
                    bindProfile(statement, diabetesType, diagnosisDate, treatmentMethod, hba1cTarget, 2);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException error) { throw databaseError("update diabetes profile", error); }
    }

    private void bindProfile(PreparedStatement statement, String diabetesType,
            LocalDate diagnosisDate, String treatmentMethod, Double hba1cTarget)
            throws SQLException {
        bindProfile(statement, diabetesType, diagnosisDate, treatmentMethod, hba1cTarget, 1);
    }

    private void bindProfile(PreparedStatement statement, String diabetesType,
            LocalDate diagnosisDate, String treatmentMethod, Double hba1cTarget, int start)
            throws SQLException {
        statement.setString(start, diabetesType);
        if (diagnosisDate == null) statement.setNull(start + 1, Types.DATE);
        else statement.setDate(start + 1, Date.valueOf(diagnosisDate));
        if (treatmentMethod == null || treatmentMethod.isBlank()) {
            statement.setNull(start + 2, Types.VARCHAR);
        } else {
            statement.setString(start + 2, treatmentMethod);
        }
        if (hba1cTarget == null) statement.setNull(start + 3, Types.NUMERIC);
        else statement.setDouble(start + 3, hba1cTarget);
    }

    private DiabetesProfile mapRow(ResultSet rows) throws SQLException {
        DiabetesProfile profile = new DiabetesProfile();
        profile.setPatientId(rows.getInt("patient_id"));
        profile.setDiabetesType(rows.getString("diabetes_type"));
        Date diagnosisDate = rows.getDate("diagnosis_date");
        profile.setDiagnosisDate(diagnosisDate == null ? null : diagnosisDate.toLocalDate());
        profile.setTreatmentMethod(rows.getString("treatment_method"));
        double target = rows.getDouble("hba1c_target");
        profile.setHba1cTarget(rows.wasNull() ? null : target);
        Timestamp updatedAt = rows.getTimestamp("updated_at");
        profile.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return profile;
    }
}