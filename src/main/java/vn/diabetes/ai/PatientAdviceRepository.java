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
        return jdbc.sql("""
                SELECT p.patient_id, p.date_of_birth,
                       COALESCE(dp.diabetes_type, 'UNKNOWN') diabetes_type,
                       dp.diagnosis_date, dp.treatment_method, dp.hba1c_target,
                       latest_indicator.hba1c latest_hba1c,
                       COALESCE((
                         SELECT jsonb_agg(jsonb_build_object(
                           'logDate', l.log_date,
                           'bloodGlucose', l.blood_glucose,
                           'systolicBp', l.systolic_bp,
                           'diastolicBp', l.diastolic_bp,
                           'weight', l.weight,
                           'mealType', l.meal_type,
                           'symptoms', l.symptoms
                         ) ORDER BY l.log_date DESC)
                         FROM patientdailylogs l
                         WHERE l.patient_id=p.patient_id AND l.log_date>=CURRENT_DATE-6
                       ), '[]'::jsonb)::text logs_json,
                       COALESCE((
                         SELECT jsonb_agg(h.condition_name)
                         FROM patient_medical_histories h
                         WHERE h.patient_id=p.patient_id AND h.status='ACTIVE'
                       ), '[]'::jsonb)::text conditions_json,
                       cached.summary cached_summary,
                       cached.advice_items_json cached_items,
                       cached.severity cached_severity,
                       cached.doctor_recommendation cached_doctor_recommendation,
                       cached.source_hash cached_source_hash,
                       cached.model cached_model,
                       cached.fallback_used cached_fallback_used
                FROM patients p
                LEFT JOIN diabetes_profiles dp ON dp.patient_id=p.patient_id
                LEFT JOIN LATERAL (
                  SELECT hi.hba1c
                  FROM healthindicators hi
                  JOIN medicalrecords mr ON mr.record_id=hi.record_id
                  WHERE mr.patient_id=p.patient_id AND hi.hba1c IS NOT NULL
                  ORDER BY hi.measured_at DESC LIMIT 1
                ) latest_indicator ON TRUE
                LEFT JOIN LATERAL (
                  SELECT summary, advice_items_json, severity, doctor_recommendation,
                         source_hash, model, fallback_used
                  FROM patient_ai_daily_advice
                  WHERE patient_id=p.patient_id AND advice_date=CURRENT_DATE
                  LIMIT 1
                ) cached ON TRUE
                WHERE p.user_id=:userId
                """)
                .param("userId", userId)
                .query((rs, rowNum) -> new Snapshot(
                        rs.getInt("patient_id"),
                        toLocalDate(rs.getDate("date_of_birth")),
                        rs.getString("diabetes_type"),
                        toLocalDate(rs.getDate("diagnosis_date")),
                        rs.getString("treatment_method"),
                        nullableDouble(rs.getObject("hba1c_target")),
                        nullableDouble(rs.getObject("latest_hba1c")),
                        parseLogs(rs.getString("logs_json")),
                        parseStrings(rs.getString("conditions_json")),
                        cache(rs.getString("cached_summary"), rs.getString("cached_items"),
                                rs.getString("cached_severity"),
                                (Boolean) rs.getObject("cached_doctor_recommendation"),
                                rs.getString("cached_source_hash"), rs.getString("cached_model"),
                                (Boolean) rs.getObject("cached_fallback_used"))))
                .optional();
    }

    public void save(int patientId, PatientAdvice advice, String sourceHash, String model, boolean fallback) {
        String items;
        try {
            items = json.writeValueAsString(advice.advice());
        } catch (Exception error) {
            throw new IllegalStateException("Cannot serialize advice", error);
        }
        jdbc.sql("""
                INSERT INTO patient_ai_daily_advice(
                  patient_id, advice_date, summary, advice_items_json, severity,
                  doctor_recommendation, source_hash, model, fallback_used)
                VALUES(:patientId, CURRENT_DATE, :summary, :items, :severity,
                       :doctorRecommendation, :sourceHash, :model, :fallback)
                ON CONFLICT(patient_id, advice_date) DO UPDATE SET
                  summary=EXCLUDED.summary,
                  advice_items_json=EXCLUDED.advice_items_json,
                  severity=EXCLUDED.severity,
                  doctor_recommendation=EXCLUDED.doctor_recommendation,
                  source_hash=EXCLUDED.source_hash,
                  model=EXCLUDED.model,
                  fallback_used=EXCLUDED.fallback_used,
                  created_at=CURRENT_TIMESTAMP
                """)
                .param("patientId", patientId)
                .param("summary", advice.summary())
                .param("items", items)
                .param("severity", advice.severity())
                .param("doctorRecommendation", advice.doctorRecommendation())
                .param("sourceHash", sourceHash)
                .param("model", model)
                .param("fallback", fallback)
                .update();
    }

    private Cache cache(String summary, String items, String severity, Boolean doctorRecommendation,
            String sourceHash, String model, Boolean fallback) {
        if (summary == null || sourceHash == null) return null;
        return new Cache(summary, parseStrings(items), severity,
                Boolean.TRUE.equals(doctorRecommendation), sourceHash, model, Boolean.TRUE.equals(fallback));
    }

    private List<DailyLog> parseLogs(String source) {
        List<DailyLog> result = new ArrayList<>();
        try {
            for (JsonNode node : json.readTree(source)) {
                result.add(new DailyLog(
                        LocalDate.parse(node.path("logDate").asText()),
                        number(node, "bloodGlucose"), integer(node, "systolicBp"),
                        integer(node, "diastolicBp"), number(node, "weight"),
                        text(node, "mealType"), text(node, "symptoms")));
            }
            return result;
        } catch (Exception error) {
            throw new IllegalStateException("Cannot read daily health snapshot", error);
        }
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

    private Double number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asDouble();
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static LocalDate toLocalDate(Date value) { return value == null ? null : value.toLocalDate(); }
    private static Double nullableDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }

    public record DailyLog(LocalDate date, Double bloodGlucose, Integer systolicBp,
            Integer diastolicBp, Double weight, String mealType, String symptoms) {}

    public record Cache(String summary, List<String> advice, String severity,
            boolean doctorRecommendation, String sourceHash, String model, boolean fallback) {}

    public record Snapshot(int patientId, LocalDate dateOfBirth, String diabetesType,
            LocalDate diagnosisDate, String treatmentMethod, Double hba1cTarget,
            Double latestHba1c, List<DailyLog> logs, List<String> conditions, Cache cache) {}
}
