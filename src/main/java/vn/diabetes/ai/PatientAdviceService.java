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
        List<String> advice = normalizeAdviceGroups(generated.advice(), prepared.fallbackAdvice());
        return new PatientAdvice(generated.summary(), advice,
                severity, doctorRecommendation, "OPENAI", false);
    }

    private List<String> normalizeAdviceGroups(List<String> generated, List<String> fallback) {
        List<String> candidates = new ArrayList<>(generated);
        candidates.addAll(fallback);
        List<String> advice = new ArrayList<>();
        appendGroup(advice, candidates, List.of("[THEO_DOI]"), 2);
        appendGroup(advice, candidates, List.of("[DIEU_TRI]"), 1);
        appendGroup(advice, candidates, List.of("[AN_UONG]"), 3);
        appendGroup(advice, candidates, List.of("[VAN_DONG]", "[CHAM_SOC]"), 3);
        appendGroup(advice, candidates, List.of("[LIEN_HE]"), 1);
        for (String value : candidates) {
            if (advice.size() >= 10) break;
            if (!hasKnownPrefix(value) && value != null && !value.isBlank() && !advice.contains(value)) {
                advice.add(value);
            }
        }
        return List.copyOf(advice);
    }

    private void appendGroup(List<String> target, List<String> candidates,
            List<String> prefixes, int maximum) {
        int added = 0;
        for (String value : candidates) {
            if (added >= maximum || target.size() >= 10) break;
            if (matchesPrefix(value, prefixes) && !target.contains(value)) {
                target.add(value);
                added++;
            }
        }
    }

    private boolean hasKnownPrefix(String value) {
        return matchesPrefix(value, List.of("[THEO_DOI]", "[DIEU_TRI]", "[AN_UONG]",
                "[VAN_DONG]", "[CHAM_SOC]", "[LIEN_HE]"));
    }

    private boolean matchesPrefix(String value, List<String> prefixes) {
        if (value == null) return false;
        String normalized = value.trim().toUpperCase();
        return prefixes.stream().anyMatch(normalized::startsWith);
    }

    private String maxSeverity(String left, String right) {
        List<String> levels = List.of("low", "medium", "high");
        int level = Math.max(levels.indexOf(left), levels.indexOf(right));
        return levels.get(Math.max(level, 0));
    }
}
