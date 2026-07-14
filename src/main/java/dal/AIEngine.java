package dal;

import models.AIWarning;
import models.HealthIndicator;
import models.PatientDailyLog;
import java.util.List;

/**
 * AIEngine — Phân tích nguy cơ tiểu đường.
 * analyze()            : rule-based đơn giản (backward compat)
 * analyzeWithHistory() : tích hợp lịch sử PatientDailyLogs + AIAdviceHistory
 */
public class AIEngine {

    // ── PHÂN TÍCH CƠ BẢN (backward compatible) ──────────────────────────
    public static AIWarning analyze(HealthIndicator h, int recordId) {
        return analyzeWithHistory(h, recordId, 0);
    }

    // ── PHÂN TÍCH CÓ LỊCH SỬ ────────────────────────────────────────────
    public static AIWarning analyzeWithHistory(HealthIndicator h, int recordId, int patientId) {
        double score = 0;
        StringBuilder warnings    = new StringBuilder();
        StringBuilder suggestions = new StringBuilder();
        StringBuilder context     = new StringBuilder();

        // ── 1. ĐƯỜNG HUYẾT ──────────────────────────────────────────────
        if (h.getBloodGlucose() >= 126) {
            score += 30;
            warnings.append("• Đường huyết ≥ 126 mg/dL — ngưỡng chẩn đoán tiểu đường.\n");
            suggestions.append("• Xét nghiệm lại đường huyết lúc đói, nghiệm pháp dung nạp glucose.\n");
        } else if (h.getBloodGlucose() >= 100) {
            score += 15;
            warnings.append("• Đường huyết " + h.getBloodGlucose() + " mg/dL — tiền tiểu đường.\n");
            suggestions.append("• Điều chỉnh chế độ ăn, giảm tinh bột, tăng vận động.\n");
        }

        // ── 2. HbA1c ────────────────────────────────────────────────────
        if (h.getHba1c() >= 6.5) {
            score += 30;
            warnings.append("• HbA1c = " + h.getHba1c() + "% — ngưỡng chẩn đoán tiểu đường type 2.\n");
            suggestions.append("• Cần điều trị bằng thuốc hạ đường huyết, theo dõi sát.\n");
        } else if (h.getHba1c() >= 5.7) {
            score += 15;
            warnings.append("• HbA1c = " + h.getHba1c() + "% — tiền tiểu đường.\n");
            suggestions.append("• Thay đổi lối sống, kiểm soát cân nặng.\n");
        }

        // ── 3. BMI ──────────────────────────────────────────────────────
        if (h.getBmi() >= 30) {
            score += 15;
            warnings.append("• BMI = " + h.getBmi() + " — béo phì, tăng nguy cơ kháng insulin.\n");
            suggestions.append("• Tư vấn dinh dưỡng, chương trình giảm cân có kiểm soát.\n");
        } else if (h.getBmi() >= 25) {
            score += 8;
            warnings.append("• BMI = " + h.getBmi() + " — thừa cân.\n");
        }

        // ── 4. HUYẾT ÁP ─────────────────────────────────────────────────
        if (h.getSystolicBp() >= 140 || h.getDiastolicBp() >= 90) {
            score += 10;
            warnings.append("• Huyết áp " + h.getSystolicBp() + "/" + h.getDiastolicBp()
                    + " mmHg — tăng huyết áp.\n");
            suggestions.append("• Đo huyết áp định kỳ, xem xét điều trị.\n");
        }

        // ── 5. CHOLESTEROL ───────────────────────────────────────────────
        if (h.getCholesterol() >= 240) {
            score += 8;
            warnings.append("• Cholesterol = " + h.getCholesterol() + " mg/dL — rối loạn mỡ máu cao.\n");
            suggestions.append("• Hạn chế mỡ động vật, xem xét dùng statin.\n");
        } else if (h.getCholesterol() >= 200) {
            score += 4;
            warnings.append("• Cholesterol = " + h.getCholesterol() + " mg/dL — ranh giới cao.\n");
        }

        // ── 6. TRIGLYCERIDE ──────────────────────────────────────────────
        if (h.getTriglyceride() >= 200) {
            score += 5;
            warnings.append("• Triglyceride = " + h.getTriglyceride() + " mg/dL — cao.\n");
        }

        // ── 7. TÍCH HỢP LỊCH SỬ PatientDailyLogs ───────────────────────
        if (patientId > 0) {
            try {
                PatientDailyLogDAO logDAO = new PatientDailyLogDAO();

                // Trung bình 7 ngày
                double[] avg7 = logDAO.getAvg7Days(patientId);
                if (avg7[0] > 0) {
                    context.append("\n[Lịch sử 7 ngày] Đường huyết TB: " + fmt1(avg7[0]) + " mg/dL");
                    if (avg7[0] >= 126) {
                        score += 10;
                        warnings.append("• Đường huyết trung bình 7 ngày cao (" + fmt1(avg7[0]) + " mg/dL).\n");
                        suggestions.append("• Kiểm soát chế độ ăn và vận động liên tục.\n");
                    } else if (avg7[0] >= 100) {
                        score += 5;
                        warnings.append("• Đường huyết trung bình 7 ngày tiền tiểu đường (" + fmt1(avg7[0]) + " mg/dL).\n");
                    }
                }
                if (avg7[1] > 0) {
                    context.append(", Huyết áp tâm thu TB: " + fmt1(avg7[1]) + " mmHg");
                    if (avg7[1] >= 140) {
                        score += 5;
                        warnings.append("• Huyết áp tâm thu TB 7 ngày cao (" + fmt1(avg7[1]) + " mmHg).\n");
                    }
                }

                // Dữ liệu hôm qua
                PatientDailyLog yesterday = logDAO.getYesterdayLog(patientId);
                if (yesterday != null && yesterday.getBloodGlucose() != null) {
                    context.append(", Đường huyết hôm qua: " + yesterday.getBloodGlucose() + " mg/dL");
                    if (yesterday.getBloodGlucose() >= 126) {
                        score += 5;
                        warnings.append("• Đường huyết hôm qua cũng cao (" + yesterday.getBloodGlucose() + " mg/dL) — xu hướng dai dẳng.\n");
                    }
                }

                // Triệu chứng lặp lại
                String[] syms = {"Mệt mỏi", "Khát nước nhiều", "Tiểu nhiều", "Chóng mặt"};
                for (String sym : syms) {
                    int d = logDAO.countConsecutiveDaysWithSymptom(patientId, sym);
                    if (d >= 3) {
                        score += 5;
                        warnings.append("• Triệu chứng '" + sym + "' kéo dài " + d + " ngày.\n");
                        suggestions.append("• Liên hệ bác sĩ về triệu chứng '" + sym + "' kéo dài.\n");
                    }
                }

            } catch (Exception e) {
                System.out.println("AIEngine.analyzeWithHistory log error: " + e.getMessage());
            }
        }

        // ── XÁC ĐỊNH MỨC ĐỘ NGUY CƠ ────────────────────────────────────
        String riskLevel;
        if      (score >= 50) riskLevel = "HIGH";
        else if (score >= 20) riskLevel = "MEDIUM";
        else                  riskLevel = "LOW";

        if (warnings.length() == 0) {
            warnings.append("• Các chỉ số sức khỏe trong giới hạn bình thường.\n");
            suggestions.append("• Duy trì lối sống lành mạnh, tái khám định kỳ.\n");
        }

        if (context.length() > 0) {
            warnings.append("\n[Dữ liệu lịch sử]").append(context);
        }

        AIWarning warning = new AIWarning();
        warning.setRecordId(recordId);
        warning.setRiskLevel(riskLevel);
        warning.setWarningMessage(warnings.toString().trim());
        warning.setSuggestedAction(suggestions.toString().trim());
        warning.setAiScore(score);
        return warning;
    }

    private static String fmt1(double v) {
        return String.format("%.1f", v);
    }
}
