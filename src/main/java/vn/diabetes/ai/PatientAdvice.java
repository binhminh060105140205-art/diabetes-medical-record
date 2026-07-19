package vn.diabetes.ai;

import java.util.List;

public record PatientAdvice(
        String summary,
        List<String> advice,
        String severity,
        boolean doctorRecommendation,
        String source,
        boolean cached) {

    public PatientAdvice {
        advice = advice == null ? List.of() : List.copyOf(advice);
    }
}
