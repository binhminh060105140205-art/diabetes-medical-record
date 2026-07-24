package vn.diabetes.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PatientAdviceRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public PatientAdviceRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public Optional<Snapshot> findSnapshotByUserId(int userId) {
        Optional<BaseSnapshot> base = jdbc.sql("""
                SELECT p.patient_id,p.date_of_birth,
                       COALESCE(dp.diabetes_type,'UNKNOWN') diabetes_type,
                       dp.diagnosis_date,dp.treatment_method,dp.hba1c_target
                FROM patients p
                LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
                WHERE p.user_id=:userId
                """)
                .param("userId", userId)
                .query((rows, rowNum) -> new BaseSnapshot(
                        rows.getInt("patient_id"),
                        toLocalDate(rows.getDate("date_of_birth")),
                        rows.getString("diabetes_type"),
                        toLocalDate(rows.getDate("diagnosis_date")),
                        rows.getString("treatment_method"),
                        nullableDouble(rows.getObject("hba1c_target"))))
                .optional();
        if (base.isEmpty()) return Optional.empty();

        BaseSnapshot patient = base.get();
        Double latestHba1c = jdbc.sql("""
                SELECT hba1c FROM (
                  SELECT hi.hba1c,
                         ROW_NUMBER() OVER (ORDER BY hi.measured_at DESC,hi.indicator_id DESC) row_number
                  FROM healthindicators hi
                  JOIN medicalrecords mr ON mr.record_id=hi.record_id
                  WHERE mr.patient_id=:patientId AND hi.hba1c IS NOT NULL
                ) latest WHERE row_number=1
                """)
                .param("patientId", patient.patientId())
                .query((rows, rowNum) -> nullableDouble(rows.getObject("hba1c")))
                .optional().orElse(null);

        List<DailyLog> logs = jdbc.sql("""
                SELECT log_date,blood_glucose,systolic_bp,diastolic_bp,weight,meal_type,symptoms
                FROM patientdailylogs
                WHERE patient_id=:patientId AND log_date>=:since
                ORDER BY log_date DESC
                """)
                .param("patientId", patient.patientId())
                .param("since", Date.valueOf(LocalDate.now().minusDays(6)))
                .query((rows, rowNum) -> new DailyLog(
                        toLocalDate(rows.getDate("log_date")),
                        nullableDouble(rows.getObject("blood_glucose")),
                        nullableInteger(rows.getObject("systolic_bp")),
                        nullableInteger(rows.getObject("diastolic_bp")),
                        nullableDouble(rows.getObject("weight")),
                        rows.getString("meal_type"), rows.getString("symptoms")))
                .list();

        List<String> conditions = jdbc.sql("""
                SELECT condition_name
                FROM patient_medical_histories
                WHERE patient_id=:patientId AND status='ACTIVE'
                ORDER BY condition_name
                """)
                .param("patientId", patient.patientId())
                .query(String.class)
                .list();

        Cache cached = jdbc.sql("""
                SELECT summary,advice_items_json,severity,doctor_recommendation,
                       source_hash,model,fallback_used
                FROM patient_ai_daily_advice
                WHERE patient_id=:patientId AND advice_date=:today
                """)
                .param("patientId", patient.patientId())
                .param("today", Date.valueOf(LocalDate.now()))
                .query((rows, rowNum) -> cache(
                        rows.getString("summary"), rows.getString("advice_items_json"),
                        rows.getString("severity"), (Boolean) rows.getObject("doctor_recommendation"),
                        rows.getString("source_hash"), rows.getString("model"),
                        (Boolean) rows.getObject("fallback_used")))
                .optional().orElse(null);

        return Optional.of(new Snapshot(
                patient.patientId(), patient.dateOfBirth(), patient.diabetesType(),
                patient.diagnosisDate(), patient.treatmentMethod(), patient.hba1cTarget(),
                latestHba1c, logs, conditions, cached));
    }

    public void save(int patientId, PatientAdvice advice, String sourceHash, String model, boolean fallback) {
        String items;
        try {
            items = json.writeValueAsString(advice.advice());
        } catch (Exception error) {
            throw new IllegalStateException("Cannot serialize advice", error);
        }
        Date today = Date.valueOf(LocalDate.now());
        int updated = jdbc.sql("""
                UPDATE patient_ai_daily_advice SET
                  summary=:summary,advice_items_json=:items,severity=:severity,
                  doctor_recommendation=:doctorRecommendation,source_hash=:sourceHash,
                  model=:model,fallback_used=:fallback,created_at=CURRENT_TIMESTAMP
                WHERE patient_id=:patientId AND advice_date=:today
                """)
                .param("patientId", patientId)
                .param("today", today)
                .param("summary", advice.summary())
                .param("items", items)
                .param("severity", advice.severity())
                .param("doctorRecommendation", advice.doctorRecommendation())
                .param("sourceHash", sourceHash)
                .param("model", model)
                .param("fallback", fallback)
                .update();
        if (updated == 0) {
            jdbc.sql("""
                    INSERT INTO patient_ai_daily_advice(
                      patient_id,advice_date,summary,advice_items_json,severity,
                      doctor_recommendation,source_hash,model,fallback_used)
                    VALUES(:patientId,:today,:summary,:items,:severity,
                           :doctorRecommendation,:sourceHash,:model,:fallback)
                    """)
                    .param("patientId", patientId)
                    .param("today", today)
                    .param("summary", advice.summary())
                    .param("items", items)
                    .param("severity", advice.severity())
                    .param("doctorRecommendation", advice.doctorRecommendation())
                    .param("sourceHash", sourceHash)
                    .param("model", model)
                    .param("fallback", fallback)
                    .update();
        }
    }

    private Cache cache(String summary, String items, String severity, Boolean doctorRecommendation,
            String sourceHash, String model, Boolean fallback) {
        if (summary == null || sourceHash == null) return null;
        return new Cache(summary, parseStrings(items), severity,
                Boolean.TRUE.equals(doctorRecommendation), sourceHash, model, Boolean.TRUE.equals(fallback));
    }

    private List<String> parseStrings(String source) {
        if (source == null || source.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        try {
            for (JsonNode node : json.readTree(source)) if (node.isTextual()) result.add(node.asText());
            return result;
        } catch (Exception error) {
            throw new IllegalStateException("Cannot read advice data", error);
        }
    }

    private static LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private static Double nullableDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }

    private static Integer nullableInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private record BaseSnapshot(int patientId, LocalDate dateOfBirth, String diabetesType,
            LocalDate diagnosisDate, String treatmentMethod, Double hba1cTarget) {}

    public record DailyLog(LocalDate date, Double bloodGlucose, Integer systolicBp,
            Integer diastolicBp, Double weight, String mealType, String symptoms) {}

    public record Cache(String summary, List<String> advice, String severity,
            boolean doctorRecommendation, String sourceHash, String model, boolean fallback) {}

    public record Snapshot(int patientId, LocalDate dateOfBirth, String diabetesType,
            LocalDate diagnosisDate, String treatmentMethod, Double hba1cTarget,
            Double latestHba1c, List<DailyLog> logs, List<String> conditions, Cache cache) {}
}