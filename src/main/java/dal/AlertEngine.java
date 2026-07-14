package dal;

import models.DeviceReading;
import models.HealthAlert;
import models.PatientDailyLog;
import java.util.ArrayList;
import java.util.List;

/**
 * [NEW - Upgrade V3]
 * Hệ thống cảnh báo RULE-BASED — không dùng AI.
 * Thay thế AIEngine trong Doctor workflow (AIEngine vẫn giữ cho Patient AI prompt).
 *
 * Gọi sau khi:
 *   - Bệnh nhân nhập chỉ số hằng ngày (PatientAIController)
 *   - Thiết bị gửi dữ liệu lên (DeviceDataUploadController)
 */
public class AlertEngine {

    // ── NGƯỠNG ĐƯỜNG HUYẾT (mg/dL) ──
    public static final double GLUCOSE_HYPO     = 70.0;
    public static final double GLUCOSE_PREDIA   = 100.0;
    public static final double GLUCOSE_HIGH     = 180.0;
    public static final double GLUCOSE_CRITICAL = 250.0;

    // ── NGƯỠNG HUYẾT ÁP (mmHg) ──
    public static final int BP_SYSTOLIC_HIGH    = 140;
    public static final int BP_SYSTOLIC_CRISIS  = 180;
    public static final int BP_DIASTOLIC_HIGH   = 90;
    public static final int BP_SYSTOLIC_LOW     = 90;

    // ── NGƯỠNG NHỊP TIM (bpm) ──
    public static final int HR_LOW      = 50;
    public static final int HR_HIGH     = 100;
    public static final int HR_CRITICAL = 130;

    // ── NGƯỠNG SPO2 (%) ──
    public static final double SPO2_LOW      = 95.0;
    public static final double SPO2_CRITICAL = 90.0;

    /** Phân tích PatientDailyLog và tạo cảnh báo */
    public static List<HealthAlert> analyzeLog(PatientDailyLog log) {
        List<HealthAlert> alerts = new ArrayList<>();
        int pid = log.getPatientId();

        // ── ĐƯỜNG HUYẾT ──
        if (log.getBloodGlucose() != null) {
            double bg = log.getBloodGlucose();
            if (bg < GLUCOSE_HYPO) {
                alerts.add(build(pid, "glucose", bg, GLUCOSE_HYPO, "high",
                    String.format("🚨 Hạ đường huyết: %.1f mg/dL (< 70). Ăn ngay 15g đường và liên hệ bác sĩ.", bg),
                    "manual", log.getLogId()));
            } else if (bg >= GLUCOSE_CRITICAL) {
                alerts.add(build(pid, "glucose", bg, GLUCOSE_CRITICAL, "high",
                    String.format("🚨 Đường huyết rất cao: %.1f mg/dL (≥ 250). Liên hệ bác sĩ ngay hôm nay.", bg),
                    "manual", log.getLogId()));
            } else if (bg >= GLUCOSE_HIGH) {
                alerts.add(build(pid, "glucose", bg, GLUCOSE_HIGH, "medium",
                    String.format("⚠️ Đường huyết cao: %.1f mg/dL (≥ 180). Hạn chế tinh bột, tăng vận động.", bg),
                    "manual", log.getLogId()));
            }
        }

        // ── HUYẾT ÁP ──
        if (log.getSystolicBp() != null) {
            int sbp = log.getSystolicBp();
            int dbp = log.getDiastolicBp() != null ? log.getDiastolicBp() : 0;
            if (sbp >= BP_SYSTOLIC_CRISIS || dbp >= 120) {
                alerts.add(build(pid, "blood_pressure", sbp, BP_SYSTOLIC_CRISIS, "high",
                    String.format("🚨 Huyết áp khủng hoảng: %d/%d mmHg. Ngồi nghỉ ngay, gọi cấp cứu nếu có triệu chứng.", sbp, dbp),
                    "manual", log.getLogId()));
            } else if (sbp >= BP_SYSTOLIC_HIGH || dbp >= BP_DIASTOLIC_HIGH) {
                alerts.add(build(pid, "blood_pressure", sbp, BP_SYSTOLIC_HIGH, "medium",
                    String.format("⚠️ Huyết áp cao: %d/%d mmHg. Nghỉ ngơi, giảm muối, uống thuốc theo đơn.", sbp, dbp),
                    "manual", log.getLogId()));
            } else if (sbp < BP_SYSTOLIC_LOW) {
                alerts.add(build(pid, "blood_pressure", sbp, BP_SYSTOLIC_LOW, "medium",
                    String.format("⚠️ Huyết áp thấp: %d/%d mmHg. Nằm nghỉ, uống nước, tránh đứng dậy đột ngột.", sbp, dbp),
                    "manual", log.getLogId()));
            }
        }

        return alerts;
    }

    /** Phân tích DeviceReading và tạo cảnh báo */
    public static List<HealthAlert> analyzeDeviceReading(DeviceReading dr) {
        List<HealthAlert> alerts = new ArrayList<>();
        int pid = dr.getPatientId();
        String dt = dr.getDeviceType();

        if (dr.getParsedGlucose() != null) {
            double bg = dr.getParsedGlucose();
            if (bg < GLUCOSE_HYPO) {
                alerts.add(build(pid, "glucose", bg, GLUCOSE_HYPO, "high",
                    String.format("🚨 [%s] Hạ đường huyết: %.1f mg/dL. Nguy hiểm — xử lý ngay.", dt, bg),
                    "device", dr.getId()));
            } else if (bg >= GLUCOSE_CRITICAL) {
                alerts.add(build(pid, "glucose", bg, GLUCOSE_CRITICAL, "high",
                    String.format("🚨 [%s] Đường huyết nguy hiểm: %.1f mg/dL (≥ 250).", dt, bg),
                    "device", dr.getId()));
            } else if (bg >= GLUCOSE_HIGH) {
                alerts.add(build(pid, "glucose", bg, GLUCOSE_HIGH, "medium",
                    String.format("⚠️ [%s] Đường huyết cao: %.1f mg/dL.", dt, bg),
                    "device", dr.getId()));
            }
        }

        if (dr.getParsedHeartRate() != null) {
            int hr = dr.getParsedHeartRate();
            if (hr >= HR_CRITICAL) {
                alerts.add(build(pid, "heart_rate", hr, HR_CRITICAL, "high",
                    String.format("🚨 [%s] Nhịp tim quá cao: %d bpm. Ngồi nghỉ, liên hệ bác sĩ.", dt, hr),
                    "device", dr.getId()));
            } else if (hr > HR_HIGH || hr < HR_LOW) {
                alerts.add(build(pid, "heart_rate", hr, HR_HIGH, "medium",
                    String.format("⚠️ [%s] Nhịp tim bất thường: %d bpm (bình thường 50–100).", dt, hr),
                    "device", dr.getId()));
            }
        }

        if (dr.getParsedSpo2() != null) {
            double spo2 = dr.getParsedSpo2();
            if (spo2 < SPO2_CRITICAL) {
                alerts.add(build(pid, "spo2", spo2, SPO2_CRITICAL, "high",
                    String.format("🚨 [%s] SpO2 nguy hiểm: %.1f%% (< 90%%). Liên hệ cấp cứu ngay.", dt, spo2),
                    "device", dr.getId()));
            } else if (spo2 < SPO2_LOW) {
                alerts.add(build(pid, "spo2", spo2, SPO2_LOW, "medium",
                    String.format("⚠️ [%s] SpO2 thấp: %.1f%% (< 95%%). Nghỉ ngơi, thở sâu.", dt, spo2),
                    "device", dr.getId()));
            }
        }

        if (dr.getParsedSystolicBp() != null) {
            int sbp = dr.getParsedSystolicBp();
            int dbp = dr.getParsedDiastolicBp() != null ? dr.getParsedDiastolicBp() : 0;
            if (sbp >= BP_SYSTOLIC_CRISIS || dbp >= 120) {
                alerts.add(build(pid, "blood_pressure", sbp, BP_SYSTOLIC_CRISIS, "high",
                    String.format("🚨 [%s] Huyết áp khủng hoảng: %d/%d mmHg.", dt, sbp, dbp),
                    "device", dr.getId()));
            } else if (sbp >= BP_SYSTOLIC_HIGH || dbp >= BP_DIASTOLIC_HIGH) {
                alerts.add(build(pid, "blood_pressure", sbp, BP_SYSTOLIC_HIGH, "medium",
                    String.format("⚠️ [%s] Huyết áp cao: %d/%d mmHg.", dt, sbp, dbp),
                    "device", dr.getId()));
            }
        }

        return alerts;
    }

    private static HealthAlert build(int pid, String type, double value, double threshold,
                                      String level, String msg, String src, int srcId) {
        HealthAlert a = new HealthAlert();
        a.setPatientId(pid);
        a.setIndicatorType(type);
        a.setValue(value);
        a.setThreshold(threshold);
        a.setAlertLevel(level);
        a.setAlertMessage(msg);
        a.setDataSource(src);
        a.setSourceRecordId(srcId);
        return a;
    }
}
