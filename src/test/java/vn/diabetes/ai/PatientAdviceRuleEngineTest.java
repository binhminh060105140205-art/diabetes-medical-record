package vn.diabetes.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PatientAdviceRuleEngineTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final PatientAdviceRuleEngine rules = new PatientAdviceRuleEngine(json);

    @Test
    void createsDifferentSafePlansForType1AndType2() {
        var type1 = rules.prepare(snapshot("TYPE_1", "INSULIN", validLogs()));
        var type2 = rules.prepare(snapshot("TYPE_2", "ORAL_MEDICATION", validLogs()));

        assertTrue(type1.fallbackAdvice().stream().anyMatch(value -> value.contains("insulin")));
        assertTrue(type1.fallbackAdvice().stream().anyMatch(value -> value.contains("hạ đường huyết")));
        assertTrue(type2.fallbackAdvice().stream().anyMatch(value -> value.contains("đồ uống nhiều đường")));
        assertTrue(type1.fallbackAdvice().size() >= 6);
        assertTrue(type2.fallbackAdvice().size() >= 6);
        assertNotEquals(type1.fallbackAdvice(), type2.fallbackAdvice());
    }

    @Test
    void excludesImpossibleMeasurementsBeforeAnalysis() {
        var invalid = List.of(new PatientAdviceRepository.DailyLog(LocalDate.now(), 900.0,
                15555, 10, 500.0, "FASTING", "Mệt mỏi"));
        var prepared = rules.prepare(snapshot("TYPE_2", "ORAL_MEDICATION", invalid));

        assertNull(prepared.context().latestGlucose());
        assertNull(prepared.context().latestSystolicBp());
        assertNull(prepared.context().latestDiastolicBp());
        assertNull(prepared.context().latestWeight());
        assertEquals(1, prepared.context().recentMeasurementDays(), "Valid symptoms remain usable");
    }

    @Test
    void serializedOutboundContextContainsNoDirectIdentityFields() throws Exception {
        var context = rules.prepare(snapshot("TYPE_2", "COMBINATION", validLogs())).context();
        String payload = json.writeValueAsString(context).toLowerCase();

        assertFalse(payload.contains("patientid"));
        assertFalse(payload.contains("userid"));
        assertFalse(payload.contains("fullname"));
        assertFalse(payload.contains("dateofbirth"));
        assertFalse(payload.contains("phone"));
        assertFalse(payload.contains("address"));
        assertFalse(payload.contains("cccd"));
        assertFalse(payload.contains("bhyt"));
        assertTrue(payload.contains("ageband"));
    }

    @Test
    void dangerSymptomsCannotBeDowngradedByTheModelLayer() {
        var logs = List.of(new PatientAdviceRepository.DailyLog(LocalDate.now(), 320.0,
                130, 80, 65.0, "AFTER_MEAL", "Đau bụng, nôn"));
        var prepared = rules.prepare(snapshot("TYPE_1", "INSULIN", logs));

        assertEquals("high", prepared.severityFloor());
        assertTrue(prepared.doctorRecommendationFloor());
    }

    private List<PatientAdviceRepository.DailyLog> validLogs() {
        return List.of(
                new PatientAdviceRepository.DailyLog(LocalDate.now(), 168.0, 138, 84, 65.0,
                        "AFTER_MEAL", "Mệt mỏi"),
                new PatientAdviceRepository.DailyLog(LocalDate.now().minusDays(1), 145.0, 134, 82, 65.2,
                        "AFTER_MEAL", "Mệt mỏi"));
    }

    private PatientAdviceRepository.Snapshot snapshot(String type, String treatment,
            List<PatientAdviceRepository.DailyLog> logs) {
        return new PatientAdviceRepository.Snapshot(999, LocalDate.now().minusYears(76), type,
                LocalDate.now().minusYears(8), treatment, 7.0, 7.4, logs,
                List.of("Tăng huyết áp"), null);
    }
}
