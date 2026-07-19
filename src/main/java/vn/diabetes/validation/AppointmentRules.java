package vn.diabetes.validation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public final class AppointmentRules {
    public static final LocalTime OPEN_TIME = LocalTime.of(7, 30);
    public static final LocalTime CLOSE_TIME = LocalTime.of(17, 30);
    public static final int SLOT_MINUTES = 30;
    public static final int MAX_PATIENTS_PER_DOCTOR_PER_DAY = 20;
    public static final int MAX_ADVANCE_DAYS = 90;

    private AppointmentRules() {}

    public static void validate(LocalDateTime appointmentAt, LocalDateTime now) {
        if (appointmentAt == null || !appointmentAt.isAfter(now.plusMinutes(15))) {
            throw new IllegalArgumentException("Lịch hẹn phải sau hiện tại ít nhất 15 phút.");
        }
        if (appointmentAt.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Phòng khám nghỉ Chủ nhật.");
        }
        if (ChronoUnit.DAYS.between(now.toLocalDate(), appointmentAt.toLocalDate()) > MAX_ADVANCE_DAYS) {
            throw new IllegalArgumentException("Chỉ được đặt lịch trong 90 ngày tới.");
        }
        LocalTime time = appointmentAt.toLocalTime();
        if (time.isBefore(OPEN_TIME) || time.isAfter(CLOSE_TIME.minusMinutes(SLOT_MINUTES))) {
            throw new IllegalArgumentException("Chỉ nhận lịch từ 07:30 đến 17:00.");
        }
        if (time.getMinute() % SLOT_MINUTES != 0 || time.getSecond() != 0 || time.getNano() != 0) {
            throw new IllegalArgumentException("Giờ hẹn phải theo khung 30 phút.");
        }
    }

    public static void validateRequestedDate(LocalDate preferredDate, LocalDate today) {
        if (preferredDate == null || !preferredDate.isAfter(today)) {
            throw new IllegalArgumentException("Ngày khám phải bắt đầu từ ngày mai.");
        }
        if (preferredDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Phòng khám nghỉ Chủ nhật.");
        }
        if (ChronoUnit.DAYS.between(today, preferredDate) > MAX_ADVANCE_DAYS) {
            throw new IllegalArgumentException("Chỉ được đăng ký khám trong 90 ngày tới.");
        }
    }

    public static void validateRequestedPeriod(String period) {
        if (!"MORNING".equals(period) && !"AFTERNOON".equals(period)) {
            throw new IllegalArgumentException("Vui lòng chọn buổi sáng hoặc buổi chiều.");
        }
    }

    public static void validateAssignmentMatchesRequest(LocalDateTime appointmentAt,
            LocalDate preferredDate, String preferredPeriod) {
        if (preferredDate == null || !appointmentAt.toLocalDate().equals(preferredDate)) {
            throw new IllegalArgumentException("Giờ khám phải nằm trong ngày bệnh nhân đã chọn.");
        }
        LocalTime time = appointmentAt.toLocalTime();
        boolean matches = "MORNING".equals(preferredPeriod)
                ? time.isBefore(LocalTime.NOON)
                : !time.isBefore(LocalTime.of(13, 0));
        if (!matches) {
            throw new IllegalArgumentException("Giờ khám không thuộc buổi bệnh nhân đã chọn.");
        }
    }
}
