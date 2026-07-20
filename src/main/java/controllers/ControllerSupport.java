package controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import models.User;
import vn.diabetes.validation.AppointmentRules;

/** Shared HTTP/session helpers for the legacy servlet controllers. */
final class ControllerSupport {
    static final String DOCTOR_ID_SESSION_KEY = "clinicDoctorId";
    static final String PATIENT_ID_SESSION_KEY = "clinicPatientId";
    private static final DateTimeFormatter APPOINTMENT_DATE_LABEL =
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter TIME_VALUE = DateTimeFormatter.ofPattern("HH:mm");

    private ControllerSupport() {}

    static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    static boolean hasRole(User user, String... roles) {
        if (user == null || user.getRole() == null) return false;
        for (String role : roles) {
            if (role.equals(user.getRole())) return true;
        }
        return false;
    }

    static void redirectToLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/Login");
    }

    static int positiveId(String value, String label) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) return parsed;
        } catch (NumberFormatException | NullPointerException ignored) {
            // Converted to the same validation error below.
        }
        throw new IllegalArgumentException(label + " không hợp lệ");
    }

    static int positiveIdOrZero(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : 0;
        } catch (NumberFormatException | NullPointerException ignored) {
            return 0;
        }
    }

    static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    static String requiredParameter(HttpServletRequest request, String name) {
        return requiredParameter(request, name, parameterLabel(name));
    }

    static String requiredParameter(HttpServletRequest request, String name, String label) {
        String value = clean(request.getParameter(name));
        if (value.isEmpty()) throw new IllegalArgumentException(label + " là bắt buộc.");
        return value;
    }

    static LocalDateTime appointmentDateTime(String dateValue, String timeValue) {
        String date = clean(dateValue);
        String time = clean(timeValue);
        if (date.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ngày khám.");
        if (time.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn khung giờ khám.");
        try {
            LocalTime parsedTime = LocalTime.parse(time, TIME_VALUE);
            if (parsedTime.getMinute() % AppointmentRules.SLOT_MINUTES != 0) {
                throw new IllegalArgumentException("Khung giờ khám phải theo khoảng 30 phút.");
            }
            return LocalDateTime.of(LocalDate.parse(date), parsedTime);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Ngày hoặc giờ khám không hợp lệ.");
        }
    }

    static List<Map<String, String>> appointmentDateOptions(boolean includeToday) {
        LocalDate today = AppointmentRules.nowInVietnam().toLocalDate();
        List<Map<String, String>> options = new ArrayList<>();
        int firstOffset = includeToday ? 0 : 1;
        for (int offset = firstOffset; offset <= AppointmentRules.MAX_ADVANCE_DAYS; offset++) {
            LocalDate date = today.plusDays(offset);
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            String prefix = offset == 0 ? "Hôm nay — " : offset == 1 ? "Ngày mai — " : "";
            Map<String, String> option = new LinkedHashMap<>();
            option.put("value", date.toString());
            option.put("label", prefix + date.format(APPOINTMENT_DATE_LABEL));
            options.add(option);
        }
        return options;
    }

    static List<Map<String, String>> appointmentTimeOptions() {
        List<Map<String, String>> options = new ArrayList<>();
        addTimeRange(options, AppointmentRules.OPEN_TIME, AppointmentRules.MORNING_END, "MORNING");
        addTimeRange(options, AppointmentRules.AFTERNOON_START, AppointmentRules.CLOSE_TIME, "AFTERNOON");
        return options;
    }

    static String parameterLabel(String name) {
        return switch (name) {
            case "patientId" -> "Bệnh nhân";
            case "doctorId" -> "Bác sĩ";
            case "appointmentId" -> "Lịch hẹn";
            case "encounterId" -> "Lượt khám";
            case "labOrderId" -> "Chỉ định xét nghiệm";
            case "reason" -> "Lý do khám";
            case "status" -> "Trạng thái";
            case "allergen" -> "Dị nguyên";
            case "conditionName" -> "Tên bệnh hoặc tiền sử";
            case "testCode" -> "Mã xét nghiệm";
            case "testName" -> "Tên xét nghiệm";
            case "resultValue" -> "Kết quả xét nghiệm";
            default -> "Thông tin";
        };
    }

    private static void addTimeRange(List<Map<String, String>> options, LocalTime start,
            LocalTime end, String period) {
        for (LocalTime time = start; time.isBefore(end);
                time = time.plusMinutes(AppointmentRules.SLOT_MINUTES)) {
            Map<String, String> option = new LinkedHashMap<>();
            option.put("value", time.format(TIME_VALUE));
            option.put("label", time.format(TIME_VALUE));
            option.put("period", period);
            options.add(option);
        }
    }

    static void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }
}
