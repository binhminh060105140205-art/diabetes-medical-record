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
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM diabetes_profiles WHERE patient_id=?")) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { throw databaseError("load diabetes profile", e); }
        return null;
    }

    /** Creates the default UNKNOWN profile for a newly registered patient. Call within the caller's transaction. */
    public void createDefault(int patientId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO diabetes_profiles(patient_id) VALUES(?) ON CONFLICT (patient_id) DO NOTHING")) {
            ps.setInt(1, patientId);
            ps.executeUpdate();
        }
    }

    /** Doctor-only update: confirms diabetes type and treatment plan for a patient. */
    public void update(int patientId, String diabetesType, LocalDate diagnosisDate,
                        String treatmentMethod, Double hba1cTarget) {
        try (PreparedStatement ps = connection.prepareStatement("""
                UPDATE diabetes_profiles SET diabetes_type=?, diagnosis_date=?, treatment_method=?,
                       hba1c_target=?, updated_at=CURRENT_TIMESTAMP
                WHERE patient_id=?""")) {
            ps.setString(1, diabetesType);
            if (diagnosisDate == null) ps.setNull(2, Types.DATE); else ps.setDate(2, Date.valueOf(diagnosisDate));
            if (treatmentMethod == null || treatmentMethod.isBlank()) ps.setNull(3, Types.VARCHAR); else ps.setString(3, treatmentMethod);
            if (hba1cTarget == null) ps.setNull(4, Types.NUMERIC); else ps.setDouble(4, hba1cTarget);
            ps.setInt(5, patientId);
            int changed = ps.executeUpdate();
            if (changed == 0) throw new IllegalArgumentException("Bệnh nhân chưa có hồ sơ tiểu đường.");
        } catch (SQLException e) { throw databaseError("update diabetes profile", e); }
    }

    private DiabetesProfile mapRow(ResultSet rs) throws SQLException {
        DiabetesProfile p = new DiabetesProfile();
        p.setPatientId(rs.getInt("patient_id"));
        p.setDiabetesType(rs.getString("diabetes_type"));
        Date diagnosisDate = rs.getDate("diagnosis_date");
        p.setDiagnosisDate(diagnosisDate == null ? null : diagnosisDate.toLocalDate());
        p.setTreatmentMethod(rs.getString("treatment_method"));
        double target = rs.getDouble("hba1c_target");
        p.setHba1cTarget(rs.wasNull() ? null : target);
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        p.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return p;
    }
}
