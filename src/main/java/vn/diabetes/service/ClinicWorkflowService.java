package vn.diabetes.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import vn.diabetes.validation.AppointmentRules;
import vn.diabetes.validation.Validators;

/** Business boundary for all write operations in the outpatient workflow. */
public class ClinicWorkflowService {
    private static final Map<String,String> LAB_TESTS = createLabTests();
    private final ClinicWorkflowGateway gateway;
    private final Supplier<LocalDateTime> clock;

    public ClinicWorkflowService(ClinicWorkflowGateway gateway) {
        this(gateway, LocalDateTime::now);
    }

    ClinicWorkflowService(ClinicWorkflowGateway gateway, Supplier<LocalDateTime> clock) {
        this.gateway = gateway;
        this.clock = clock;
    }

    public void createAppointment(int patientId, int doctorId, LocalDateTime at,
            String reason, String note, int actor) {
        positive(patientId, "Bệnh nhân");
        positive(doctorId, "Bác sĩ");
        AppointmentRules.validate(at, clock.get());
        reason = Validators.required(reason, "Lý do khám");
        if (reason.length() > 255) throw new IllegalArgumentException("Lý do khám tối đa 255 ký tự.");
        gateway.createAppointment(patientId, doctorId, at, reason,
                Validators.max(note, 500, "Ghi chú"), actor);
    }

    public void createAppointmentRequest(int patientId, LocalDate preferredDate,
            String preferredPeriod, String reason, String note, int actor) {
        positive(patientId, "Bệnh nhân");
        preferredPeriod = preferredPeriod == null ? "" : preferredPeriod.trim().toUpperCase();
        AppointmentRules.validateRequestedDate(preferredDate, clock.get().toLocalDate());
        AppointmentRules.validateRequestedPeriod(preferredPeriod);
        reason = Validators.required(reason, "Lý do khám");
        if (reason.length() > 255) throw new IllegalArgumentException("Lý do khám tối đa 255 ký tự.");
        gateway.createAppointmentRequest(patientId, preferredDate, preferredPeriod, reason,
                Validators.max(note, 500, "Ghi chú"), actor);
    }

    public void assignAppointmentRequest(int appointmentId, int doctorId,
            LocalDateTime at, String note, int actor) {
        positive(appointmentId, "Yêu cầu khám");
        positive(doctorId, "Bác sĩ");
        AppointmentRules.validate(at, clock.get());
        gateway.assignAppointmentRequest(appointmentId, doctorId, at,
                Validators.max(note, 500, "Ghi chú"), actor);
    }

    public void rescheduleAppointment(int id, LocalDateTime at, String note, int actor) {
        positive(id, "Lịch hẹn");
        AppointmentRules.validate(at, clock.get());
        gateway.rescheduleAppointment(id, at, Validators.max(note, 500, "Ghi chú"), actor);
    }

    public void setAppointmentStatus(int id, String status, int actor) {
        positive(id, "Lịch hẹn");
        gateway.setAppointmentStatus(id, Validators.required(status, "Trạng thái"), actor);
    }

    public void checkIn(int id, int actor) {
        positive(id, "Lịch hẹn");
        gateway.checkIn(id, actor);
    }

    public void cancelOwnAppointment(int id, int patientUserId, int actor) {
        positive(id, "Lịch hẹn");
        positive(patientUserId, "Tài khoản bệnh nhân");
        gateway.cancelOwnAppointment(id, patientUserId, actor);
    }

    public void setEncounterStatus(int id, String status, int actor) {
        positive(id, "Lượt khám");
        gateway.setEncounterStatus(id, status, actor);
    }

    public void addAllergy(int patientId, String allergen, String reaction,
            String severity, int actor) {
        positive(patientId, "Bệnh nhân");
        gateway.addAllergy(patientId, Validators.required(allergen, "Dị nguyên"),
                Validators.max(reaction, 255, "Phản ứng"), severity, actor);
    }

    public void addHistory(int patientId, String type, String name, Date date,
            String status, String note, int actor) {
        positive(patientId, "Bệnh nhân");
        gateway.addHistory(patientId, type, Validators.required(name, "Tên bệnh"),
                date, status, Validators.max(note, 500, "Ghi chú"), actor);
    }

    public void createLabOrder(int encounterId, int doctorId, String code,
            String name, String priority, String note, int actor) {
        code = code == null ? "" : code.trim().toUpperCase();
        name = LAB_TESTS.get(code);
        if (name == null) throw new IllegalArgumentException("Xét nghiệm không nằm trong danh mục hỗ trợ.");
        priority = priority == null ? "ROUTINE" : priority.trim().toUpperCase();
        if (!Set.of("ROUTINE", "URGENT").contains(priority))
            throw new IllegalArgumentException("Mức ưu tiên xét nghiệm không hợp lệ.");
        positive(encounterId, "Lượt khám");
        positive(doctorId, "Bác sĩ");
        gateway.createLabOrder(encounterId, doctorId,
                Validators.required(code, "Mã xét nghiệm"),
                Validators.required(name, "Tên xét nghiệm"), priority,
                Validators.max(note, 500, "Ghi chú"), actor);
    }

    public void resultLab(int orderId, String value, String unit,
            String range, String flag, int actor) {
        flag = flag == null ? "NORMAL" : flag.trim().toUpperCase();
        if (!Set.of("NORMAL", "HIGH", "LOW", "CRITICAL").contains(flag))
            throw new IllegalArgumentException("Cờ kết quả không hợp lệ.");
        positive(orderId, "Chỉ định");
        gateway.resultLab(orderId, Validators.required(value, "Kết quả"),
                unit, range, flag, actor);
    }

    public static Map<String,String> labTests() { return LAB_TESTS; }

    private static Map<String,String> createLabTests() {
        Map<String,String> tests = new LinkedHashMap<>();
        // Aliases kept for the short codes already used by the workflow UI.
        tests.put("GLU", "Duong huyet");
        tests.put("CRE", "Creatinine - chuc nang than");
        tests.put("UACR", "Albumin/Creatinine nieu");
        tests.put("GLU_FASTING", "Đường huyết lúc đói");
        tests.put("HBA1C", "HbA1c");
        tests.put("LIPID", "Mỡ máu (Lipid máu)");
        tests.put("CREATININE", "Creatinine - chức năng thận");
        tests.put("EGFR", "eGFR - mức lọc cầu thận");
        tests.put("URINE_ALBUMIN", "Albumin niệu");
        return Collections.unmodifiableMap(tests);
    }

    private void positive(int id, String label) {
        if (id <= 0) throw new IllegalArgumentException(label + " không hợp lệ.");
    }
}
