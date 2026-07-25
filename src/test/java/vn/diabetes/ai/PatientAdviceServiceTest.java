package vn.diabetes.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PatientAdviceServiceTest {

    @Test
    void fillsRequiredGroupsWhenGeneratedAdviceOmitsThem() {
        PatientAdviceRepository repository = mock(PatientAdviceRepository.class);
        OpenAiAdviceClient openAi = mock(OpenAiAdviceClient.class);
        PatientAdviceRuleEngine rules = new PatientAdviceRuleEngine(
                new ObjectMapper().findAndRegisterModules());
        PatientAdviceRepository.Snapshot snapshot = new PatientAdviceRepository.Snapshot(
                7, LocalDate.of(1950, 1, 1), "TYPE_1", LocalDate.of(2015, 3, 1),
                "INSULIN", 6.5, 6.8,
                List.of(new PatientAdviceRepository.DailyLog(LocalDate.now(), 115.0,
                        128, 78, 62.0, "FASTING", "Không có")),
                List.of(), null);
        when(repository.findSnapshotByUserId(21)).thenReturn(Optional.of(snapshot));
        when(openAi.isConfigured()).thenReturn(true);
        when(openAi.model()).thenReturn("test-model");
        when(openAi.generate(any())).thenReturn(new OpenAiAdviceClient.GeneratedAdvice(
                "Duy trì theo dõi đều đặn.",
                List.of(
                        "[THEO_DOI] Ghi đường huyết hôm nay.",
                        "[DIEU_TRI] Dùng insulin đúng đơn.",
                        "[AN_UONG] Ăn đúng bữa.",
                        "[VAN_DONG] Đi bộ nhẹ nếu cơ thể ổn."),
                "low", false));

        PatientAdvice result = new PatientAdviceService(repository, rules, openAi)
                .getDailyAdvice(21);

        assertTrue(result.advice().size() >= 8 && result.advice().size() <= 10);
        assertTrue(countPrefix(result, "[THEO_DOI]") >= 1);
        assertTrue(countPrefix(result, "[DIEU_TRI]") >= 1);
        assertTrue(countPrefix(result, "[AN_UONG]") >= 2);
        assertTrue(countPrefix(result, "[VAN_DONG]") + countPrefix(result, "[CHAM_SOC]") >= 1);
        assertTrue(countPrefix(result, "[LIEN_HE]") >= 1);
        verify(repository).save(eq(7), any(PatientAdvice.class), any(String.class),
                eq("test-model"), eq(false));
    }

    private long countPrefix(PatientAdvice advice, String prefix) {
        return advice.advice().stream().filter(value -> value.startsWith(prefix)).count();
    }
}
