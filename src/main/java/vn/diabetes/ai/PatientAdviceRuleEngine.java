package vn.diabetes.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PatientAdviceRuleEngine {
    private static final String ADVICE_VERSION = "daily-advice-v2-full";
    private static final Map<String, String> SYMPTOMS = Map.ofEntries(
            Map.entry("met moi", "fatigue"), Map.entry("khat nhieu", "excessive_thirst"),
            Map.entry("chong mat", "dizziness"), Map.entry("run tay", "tremor_or_sweating"),
            Map.entry("va mo hoi", "tremor_or_sweating"), Map.entry("buon non", "nausea"),
            Map.entry("dau bung", "abdominal_pain"),
            Map.entry("kho tho", "abnormal_breathing"), Map.entry("tho bat thuong", "abnormal_breathing"),
            Map.entry("lu lan", "confusion"), Map.entry("ngat", "fainting"));
    private final ObjectMapper json;

    public PatientAdviceRuleEngine(ObjectMapper json) { this.json = json; }

    public Prepared prepare(PatientAdviceRepository.Snapshot snapshot) {
        List<ValidLog> valid = snapshot.logs().stream().map(this::validate).toList();
        ValidLog latest = valid.stream().filter(ValidLog::hasAnyValue).findFirst().orElse(null);
        List<Double> glucoseValues = valid.stream().map(ValidLog::bloodGlucose).filter(v -> v != null).toList();
        Double latestGlucose = latest == null ? null : latest.bloodGlucose();
        Double averageGlucose = average(glucoseValues);
        String glucoseTrend = trend(glucoseValues);
        Set<String> symptomCodes = new LinkedHashSet<>();
        Map<String, Integer> symptomCounts = new LinkedHashMap<>();
        valid.forEach(log -> log.symptoms().forEach(code -> {
            symptomCodes.add(code);
            symptomCounts.merge(code, 1, Integer::sum);
        }));
        boolean repeatedSymptoms = symptomCounts.values().stream().anyMatch(count -> count >= 2);
        Map<String, Boolean> comorbidities = comorbidityFlags(snapshot.conditions());

        SanitizedContext context = new SanitizedContext(
                ageBand(snapshot.dateOfBirth()), durationBand(snapshot.diagnosisDate()),
                safeType(snapshot.diabetesType()), safeTreatment(snapshot.treatmentMethod()),
                within(snapshot.hba1cTarget(), 4, 15), within(snapshot.latestHba1c(), 3, 25),
                latestGlucose, averageGlucose, glucoseTrend,
                latest == null ? null : latest.systolicBp(), latest == null ? null : latest.diastolicBp(),
                latest == null ? null : latest.weight(), latest == null ? null : latest.mealType(),
                List.copyOf(symptomCodes), repeatedSymptoms, comorbidities,
                (int) valid.stream().filter(ValidLog::hasAnyValue).count());

        String severity = severity(context);
        boolean doctorRecommendation = !"low".equals(severity) &&
                ("high".equals(severity) || repeatedSymptoms || isRepeatedOutlier(glucoseValues));
        List<String> fallback = fallbackAdvice(context, doctorRecommendation);
        String summary = fallbackSummary(context, severity);
        return new Prepared(context, hash(context), severity, doctorRecommendation, summary, fallback);
    }

    private ValidLog validate(PatientAdviceRepository.DailyLog log) {
        return new ValidLog(
                ChronoUnit.DAYS.between(log.date(), LocalDate.now()),
                within(log.bloodGlucose(), 40, 500), within(log.systolicBp(), 70, 250),
                within(log.diastolicBp(), 40, 150), within(log.weight(), 25, 200),
                safeMealType(log.mealType()), normalizeSymptoms(log.symptoms()));
    }

    private List<String> normalizeSymptoms(String value) {
        if (value == null || value.isBlank() || normalize(value).contains("khong co")) return List.of();
        String normalized = normalize(value);
        Set<String> result = new LinkedHashSet<>();
        SYMPTOMS.forEach((phrase, code) -> { if (normalized.contains(phrase)) result.add(code); });
        if ((normalized.contains("non") || normalized.contains("oi")) && !normalized.contains("buon non")) {
            result.add("vomiting");
        }
        return List.copyOf(result);
    }

    private Map<String, Boolean> comorbidityFlags(List<String> conditions) {
        String all = normalize(String.join(" ", conditions));
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("hypertension", all.contains("tang huyet ap"));
        flags.put("heart_disease", containsAny(all, "tim mach", "benh tim", "suy tim", "mach vanh"));
        flags.put("kidney_disease", containsAny(all, "benh than", "suy than", "than man"));
        flags.put("vision_complication", containsAny(all, "benh mat", "võng mac", "vong mac"));
        flags.put("foot_or_neuropathy", containsAny(all, "ban chan", "than kinh ngoai bien", "loet chan"));
        flags.put("obesity", containsAny(all, "beo phi", "thua can"));
        return flags;
    }

    private String severity(SanitizedContext c) {
        Set<String> symptoms = Set.copyOf(c.symptoms());
        boolean dangerSymptom = symptoms.contains("confusion") || symptoms.contains("fainting")
                || symptoms.contains("abnormal_breathing");
        boolean stomachCluster = symptoms.contains("vomiting") || symptoms.contains("abdominal_pain")
                || symptoms.contains("nausea");
        if (dangerSymptom || (c.latestGlucose() != null && c.latestGlucose() < 54)
                || (c.latestGlucose() != null && c.latestGlucose() >= 300 && stomachCluster)
                || (c.latestSystolicBp() != null && c.latestSystolicBp() >= 180)
                || (c.latestDiastolicBp() != null && c.latestDiastolicBp() >= 120)) return "high";
        if ((c.latestGlucose() != null && (c.latestGlucose() < 70 || c.latestGlucose() >= 250))
                || (c.latestSystolicBp() != null && c.latestSystolicBp() >= 160)
                || (c.latestDiastolicBp() != null && c.latestDiastolicBp() >= 100)
                || c.repeatedSymptoms() || isRepeatedOutlierFromContext(c)) return "medium";
        return "low";
    }

    private boolean isRepeatedOutlierFromContext(SanitizedContext c) {
        return c.recentMeasurementDays() >= 2 && ("rising".equals(c.glucoseTrend())
                || "falling".equals(c.glucoseTrend())) && c.averageGlucose7d() != null
                && (c.averageGlucose7d() < 70 || c.averageGlucose7d() >= 250);
    }

    private boolean isRepeatedOutlier(List<Double> values) {
        return values.stream().filter(v -> v < 70 || v >= 250).count() >= 2;
    }

    private List<String> fallbackAdvice(SanitizedContext c, boolean doctorRecommendation) {
        List<String> advice = new ArrayList<>();
        if (c.recentMeasurementDays() == 0) {
            advice.add("Hãy nhập đường huyết và huyết áp hôm nay để hệ thống theo dõi sát hơn.");
        } else if ("rising".equals(c.glucoseTrend())) {
            advice.add("Đường huyết gần đây có xu hướng tăng; hãy đo đúng thời điểm và ghi lại bữa ăn, triệu chứng.");
        } else if ("falling".equals(c.glucoseTrend())) {
            advice.add("Đường huyết gần đây có xu hướng giảm; nên theo dõi sát và báo người thân nếu thấy run tay, vã mồ hôi hoặc chóng mặt.");
        } else {
            advice.add("Tiếp tục ghi chỉ số đều đặn vào cùng thời điểm mỗi ngày để dễ so sánh.");
        }
        if ("TYPE_1".equals(c.diabetesType())) {
            advice.add("Dùng insulin đúng đơn, đúng loại và đúng giờ; không tự đổi liều, bỏ mũi tiêm hoặc tiêm bù khi chưa hỏi nhân viên y tế.");
            advice.add("Ăn đúng bữa, chuẩn bị sẵn nguồn đường hấp thu nhanh theo hướng dẫn đã được bác sĩ dặn và chú ý dấu hiệu hạ đường huyết như run tay, vã mồ hôi, chóng mặt hoặc lú lẫn.");
        } else if ("TYPE_2".equals(c.diabetesType())) {
            advice.add("Dùng thuốc hoặc insulin đúng hướng dẫn; không tự ngừng thuốc khi chỉ số thay đổi hoặc khi cảm thấy khỏe hơn.");
            advice.add("Ăn đúng bữa, ưu tiên rau và thực phẩm ít chế biến; hạn chế đồ uống nhiều đường như nước ngọt, trà sữa, cùng bánh kẹo và khẩu phần tinh bột quá lớn.");
            if (!Boolean.TRUE.equals(c.comorbidities().get("heart_disease"))
                    && !Boolean.TRUE.equals(c.comorbidities().get("kidney_disease"))) {
                advice.add("Nếu cơ thể ổn và bác sĩ không dặn hạn chế, có thể đi bộ hoặc vận động nhẹ theo khả năng; dừng lại khi chóng mặt, khó thở hay đau ngực.");
            } else {
                advice.add("Vì có bệnh tim hoặc thận đi kèm, hãy thực hiện ăn uống, lượng nước và vận động theo hướng dẫn riêng của bác sĩ.");
            }
        } else {
            advice.add("Chưa xác định loại tiểu đường; hãy hỏi bác sĩ trong lần khám tới và không tự thay đổi điều trị.");
        }
        if (doctorRecommendation) {
            advice.add("Nên liên hệ bác sĩ hoặc phòng khám để được hướng dẫn phù hợp, đặc biệt khi triệu chứng lặp lại hoặc chỉ số tiếp tục bất thường.");
        }
        if (c.latestSystolicBp() != null || c.latestDiastolicBp() != null) {
            advice.add("Tiếp tục đo huyết áp khi đã nghỉ yên, ngồi đúng tư thế và ghi lại kết quả để bác sĩ so sánh giữa các ngày.");
        } else {
            advice.add("Nếu có máy đo, nên ghi thêm huyết áp trong ngày để theo dõi đồng thời nguy cơ tim mạch.");
        }
        if ("75_84".equals(c.ageBand()) || "85_plus".equals(c.ageBand())) {
            advice.add("Nên có người thân hỗ trợ theo dõi thuốc, bữa ăn và các dấu hiệu bất thường; ưu tiên an toàn, nghỉ ngơi và tránh vận động một mình khi không khỏe.");
        }
        advice.add("Kiểm tra bàn chân mỗi ngày, giữ da sạch và khô; không tự xử lý vết phồng, vết loét hoặc vùng đỏ đau kéo dài.");
        advice.add("Ngủ đủ, hạn chế thức khuya và ghi lại bữa ăn, thời điểm dùng thuốc cùng triệu chứng để lần khám sau bác sĩ dễ đánh giá.");
        if (c.physicianHba1cTarget() != null) {
            advice.add("Mục tiêu HbA1c do bác sĩ đặt là " + c.physicianHba1cTarget()
                    + "%; tiếp tục theo kế hoạch điều trị và tái khám để đánh giá mức kiểm soát dài hạn.");
        } else {
            advice.add("Hãy hỏi bác sĩ về mục tiêu HbA1c phù hợp với tuổi, loại tiểu đường và bệnh đi kèm trong lần tái khám.");
        }
        return advice.stream().distinct().limit(8).toList();
    }

    private String fallbackSummary(SanitizedContext c, String severity) {
        if (c.recentMeasurementDays() == 0) return "Chưa có đủ chỉ số gần đây để nhận xét xu hướng.";
        return switch (severity) {
            case "high" -> "Có dấu hiệu cần được nhân viên y tế đánh giá sớm.";
            case "medium" -> "Một số chỉ số hoặc triệu chứng cần được chú ý và theo dõi sát.";
            default -> "Chưa phát hiện dấu hiệu nổi bật từ dữ liệu hợp lệ đã nhập.";
        };
    }

    private String hash(SanitizedContext context) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(ADVICE_VERSION.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] digest = messageDigest.digest(json.writeValueAsBytes(context));
            StringBuilder value = new StringBuilder();
            for (byte b : digest) value.append(String.format("%02x", b));
            return value.toString();
        } catch (Exception error) {
            throw new IllegalStateException("Cannot fingerprint advice context", error);
        }
    }

    private String trend(List<Double> values) {
        if (values.size() < 2) return "insufficient";
        double latest = values.get(0);
        double previousAverage = values.subList(1, values.size()).stream().mapToDouble(Double::doubleValue).average().orElse(latest);
        if (previousAverage == 0) return "insufficient";
        double change = (latest - previousAverage) / previousAverage;
        if (change >= 0.10) return "rising";
        if (change <= -0.10) return "falling";
        return "stable";
    }

    private Double average(List<Double> values) {
        return values.isEmpty() ? null : round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    private Double round(double value) { return Math.round(value * 10.0) / 10.0; }
    private String ageBand(LocalDate birthDate) {
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) return "unknown";
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 65) return "under_65";
        if (age < 75) return "65_74";
        if (age < 85) return "75_84";
        return "85_plus";
    }
    private String durationBand(LocalDate diagnosisDate) {
        if (diagnosisDate == null || diagnosisDate.isAfter(LocalDate.now())) return "unknown";
        int years = Period.between(diagnosisDate, LocalDate.now()).getYears();
        if (years < 1) return "under_1_year";
        if (years <= 5) return "1_5_years";
        if (years <= 10) return "6_10_years";
        return "over_10_years";
    }
    private String safeType(String value) {
        return "TYPE_1".equals(value) || "TYPE_2".equals(value) ? value : "UNKNOWN";
    }
    private String safeTreatment(String value) {
        return "INSULIN".equals(value) || "ORAL_MEDICATION".equals(value)
                || "LIFESTYLE".equals(value) || "COMBINATION".equals(value) ? value : "UNKNOWN";
    }
    private String safeMealType(String value) {
        return "FASTING".equals(value) || "AFTER_MEAL".equals(value)
                || "BEDTIME".equals(value) || "OTHER".equals(value) ? value : "UNKNOWN";
    }
    private Double within(Double value, double min, double max) { return value != null && value >= min && value <= max ? value : null; }
    private Integer within(Integer value, int min, int max) { return value != null && value >= min && value <= max ? value : null; }
    private boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }
    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
    }

    private record ValidLog(long daysAgo, Double bloodGlucose, Integer systolicBp,
            Integer diastolicBp, Double weight, String mealType, List<String> symptoms) {
        boolean hasAnyValue() {
            return bloodGlucose != null || systolicBp != null || diastolicBp != null || weight != null || !symptoms.isEmpty();
        }
    }

    public record SanitizedContext(String ageBand, String diabetesDurationBand, String diabetesType,
            String treatmentMethod, Double physicianHba1cTarget, Double latestHba1c,
            Double latestGlucose, Double averageGlucose7d, String glucoseTrend,
            Integer latestSystolicBp, Integer latestDiastolicBp, Double latestWeight,
            String measurementTiming, List<String> symptoms, boolean repeatedSymptoms,
            Map<String, Boolean> comorbidities, int recentMeasurementDays) {}

    public record Prepared(SanitizedContext context, String sourceHash, String severityFloor,
            boolean doctorRecommendationFloor, String fallbackSummary, List<String> fallbackAdvice) {}
}
