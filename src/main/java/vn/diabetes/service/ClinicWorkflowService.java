package vn.diabetes.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import vn.diabetes.validation.AppointmentRules;
import vn.diabetes.validation.Validators;

/** Business boundary for all write operations in the outpatient workflow. */
public class ClinicWorkflowService {
    private static final Map<String,String> LAB_TESTS = createLabTests();
    private static final Set<String> APPOINTMENT_STATUSES =
            Set.of("CONFIRMED", "CANCELLED", "NO_SHOW");
    private static final Set<String> ENCOUNTER_STATUSES = Set.of(
            "WAITING_TRIAGE", "WAITING_DOCTOR", "IN_CONSULTATION", "WAITING_LAB",
            "LAB_COMPLETED", "COMPLETED", "CANCELLED");
    private static final Set<String> ALLERGY_SEVERITIES =
            Set.of("MILD", "MODERATE", "SEVERE", "UNKNOWN");
    private static final Set<String> HISTORY_TYPES =
            Set.of("PERSONAL", "FAMILY", "SURGICAL", "LIFESTYLE");
    private static final Set<String> HISTORY_STATUSES = Set.of("ACTIVE", "RESOLVED");
    private final ClinicWorkflowGateway gateway;
    private final Supplier<LocalDateTime> clock;

    public ClinicWorkflowService(ClinicWorkflowGateway gateway) {
        this(gateway, AppointmentRules::nowInVietnam);
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
        // Yêu cầu từ trang bệnh nhân chỉ ghi ngày/buổi mong muốn; chưa phân công bác sĩ ở bước này.
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
        status = Validators.required(status, "Trạng thái").toUpperCase();
        if (!APPOINTMENT_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Trạng thái lịch hẹn không hợp lệ.");
        }
        gateway.setAppointmentStatus(id, status, actor);
    }

    public void checkIn(int id, int actor) {
        // Check-in chuyển lịch đã xác nhận thành lượt khám; gateway đảm bảo tạo encounter trong một transaction.
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
        status = Validators.required(status, "Trạng thái").toUpperCase();
        if (!ENCOUNTER_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Trạng thái lượt khám không hợp lệ.");
        }
        gateway.setEncounterStatus(id, status, actor);
    }

    public void addAllergy(int patientId, String allergen, String reaction,
            String severity, int actor) {
        positive(patientId, "Bệnh nhân");
        severity = severity == null ? "UNKNOWN" : severity.trim().toUpperCase();
        if (!ALLERGY_SEVERITIES.contains(severity)) {
            throw new IllegalArgumentException("Mức độ dị ứng không hợp lệ.");
        }
        gateway.addAllergy(patientId,
                Validators.max(Validators.required(allergen, "Dị nguyên"), 150, "Dị nguyên"),
                Validators.max(reaction, 255, "Phản ứng"), severity, actor);
    }

    public void addHistory(int patientId, String type, String name, Date date,
            String status, String note, int actor) {
        positive(patientId, "Bệnh nhân");
        type = type == null ? "" : type.trim().toUpperCase();
        status = status == null ? "ACTIVE" : status.trim().toUpperCase();
        if (!HISTORY_TYPES.contains(type)) {
            throw new IllegalArgumentException("Loại tiền sử không hợp lệ.");
        }
        if (!HISTORY_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Trạng thái tiền sử không hợp lệ.");
        }
        if (date != null && date.toLocalDate().isAfter(clock.get().toLocalDate())) {
            throw new IllegalArgumentException("Ngày phát hiện không được ở tương lai.");
        }
        gateway.addHistory(patientId, type,
                Validators.max(Validators.required(name, "Tên bệnh"), 150, "Tên bệnh"),
                date, status, Validators.max(note, 500, "Ghi chú"), actor);
    }

    public void createLabOrder(int encounterId, int doctorId, String code,
            String name, String priority, String note, int actor) {
        createLabOrders(encounterId, doctorId, new String[]{code}, priority, note, actor);
    }

    public void createLabOrders(int encounterId, int doctorId, String[] codes,
            String priority, String note, int actor) {
        // Một lần chỉ định có thể có nhiều mã xét nghiệm; loại trùng trước khi ghi từng order xuống database.
        if (codes == null || codes.length == 0) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một xét nghiệm.");
        }
        priority = priority == null ? "ROUTINE" : priority.trim().toUpperCase();
        if (!Set.of("ROUTINE", "URGENT").contains(priority))
            throw new IllegalArgumentException("Mức ưu tiên xét nghiệm không hợp lệ.");
        positive(encounterId, "Lượt khám");
        positive(doctorId, "Bác sĩ");

        Set<String> uniqueCodes = new LinkedHashSet<>();
        for (String rawCode : codes) {
            String code = rawCode == null ? "" : rawCode.trim().toUpperCase();
            String name = LAB_TESTS.get(code);
            if (name == null) {
                throw new IllegalArgumentException("Xét nghiệm không nằm trong danh mục hỗ trợ.");
            }
            uniqueCodes.add(code);
        }

        if (uniqueCodes.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một xét nghiệm.");
        }
        String validatedNote = Validators.max(note, 500, "Ghi chú");
        for (String code : uniqueCodes) {
            gateway.createLabOrder(encounterId, doctorId, code, LAB_TESTS.get(code),
                    priority, validatedNote, actor);
        }
    }

    public void resultLab(int orderId, String value, String unit,
            String range, String flag, int actor) {
        // Staff nhập hoặc import kết quả sau khi xét nghiệm; service kiểm tra cờ và giới hạn chuỗi trước khi lưu.
        flag = flag == null ? "NORMAL" : flag.trim().toUpperCase();
        if (!Set.of("NORMAL", "HIGH", "LOW", "CRITICAL").contains(flag))
            throw new IllegalArgumentException("Cờ kết quả không hợp lệ.");
        positive(orderId, "Chỉ định");
        gateway.resultLab(orderId,
                Validators.max(Validators.required(value, "Kết quả"), 100, "Kết quả"),
                Validators.max(unit, 30, "Đơn vị"),
                Validators.max(range, 100, "Khoảng tham chiếu"), flag, actor);
    }

    public static Map<String,String> labTests() { return LAB_TESTS; }

    private static Map<String,String> createLabTests() {
        Map<String,String> tests = new LinkedHashMap<>();
        // Danh mục xét nghiệm ban đầu dùng thống nhất với sáu trường trong hồ sơ bệnh án.
        tests.put("GLU_FASTING", "Đường huyết lúc đói");
        tests.put("HBA1C", "HbA1c");
        tests.put("CHOLESTEROL", "Cholesterol");
        tests.put("TRIGLYCERIDE", "Triglyceride");
        tests.put("HDL_C", "HDL-C");
        tests.put("LDL_C", "LDL-C");
        return Collections.unmodifiableMap(tests);
    }

    private void positive(int id, String label) {
        if (id <= 0) throw new IllegalArgumentException(label + " không hợp lệ.");
    }
}
