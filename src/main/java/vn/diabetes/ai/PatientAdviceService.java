package vn.diabetes.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

@Service
public class PatientAdviceService {
    private static final Logger LOGGER = Logger.getLogger(PatientAdviceService.class.getName());
    private final PatientAdviceRepository repository;
    private final PatientAdviceRuleEngine rules;
    private final OpenAiAdviceClient openAi;

    public PatientAdviceService(PatientAdviceRepository repository, PatientAdviceRuleEngine rules,
            OpenAiAdviceClient openAi) {
        this.repository = repository;
        this.rules = rules;
        this.openAi = openAi;
    }

    public PatientAdvice getDailyAdvice(int userId) {
        PatientAdviceRepository.Snapshot snapshot = repository.findSnapshotByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("Chưa có hồ sơ bệnh nhân."));
        PatientAdviceRuleEngine.Prepared prepared = rules.prepare(snapshot);
        PatientAdviceRepository.Cache cache = snapshot.cache();
        if (cache != null && prepared.sourceHash().equals(cache.sourceHash())
                && (!cache.fallback() || !openAi.isConfigured())) {
            return new PatientAdvice(cache.summary(), cache.advice(), cache.severity(),
                    cache.doctorRecommendation(), cache.fallback() ? "LOCAL_RULES" : "OPENAI", true);
        }

        OpenAiAdviceClient.GeneratedAdvice generated = null;
        if (openAi.isConfigured()) {
            try {
                generated = openAi.generate(prepared);
            } catch (RuntimeException error) {
                LOGGER.log(Level.WARNING, "Daily advice provider unavailable: {0}", error.getClass().getSimpleName());
            }
        }

        boolean fallback = generated == null;
        PatientAdvice result = fallback
                ? new PatientAdvice(prepared.fallbackSummary(), prepared.fallbackAdvice(),
                        prepared.severityFloor(), prepared.doctorRecommendationFloor(), "LOCAL_RULES", false)
                : mergeSafety(generated, prepared);
        repository.save(snapshot.patientId(), result, prepared.sourceHash(),
                fallback ? "local-rules-v1" : openAi.model(), fallback);
        return result;
    }

    private PatientAdvice mergeSafety(OpenAiAdviceClient.GeneratedAdvice generated,
            PatientAdviceRuleEngine.Prepared prepared) {
        String severity = maxSeverity(generated.severity(), prepared.severityFloor());
        boolean doctorRecommendation = generated.doctorRecommendation()
                || prepared.doctorRecommendationFloor() || "high".equals(severity);
        List<String> advice = new ArrayList<>(generated.advice());
        if (doctorRecommendation && advice.stream().noneMatch(this::mentionsClinician)) {
            advice.add("Nên liên hệ bác sĩ hoặc phòng khám để được hướng dẫn phù hợp.");
        }
        return new PatientAdvice(generated.summary(), advice.stream().distinct().limit(8).toList(),
                severity, doctorRecommendation, "OPENAI", false);
    }

    private boolean mentionsClinician(String value) {
        String lower = value == null ? "" : value.toLowerCase();
        return lower.contains("bác sĩ") || lower.contains("phòng khám") || lower.contains("y tế");
    }

    private String maxSeverity(String left, String right) {
        List<String> levels = List.of("low", "medium", "high");
        int level = Math.max(levels.indexOf(left), levels.indexOf(right));
        return levels.get(Math.max(level, 0));
    }
}
