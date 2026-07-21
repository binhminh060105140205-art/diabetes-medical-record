package vn.diabetes.validation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public final class AppointmentRules {
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final LocalTime OPEN_TIME = LocalTime.of(7, 30);
    public static final LocalTime MORNING_END = LocalTime.NOON;
    public static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    public static final LocalTime CLOSE_TIME = LocalTime.of(17, 30);
    public static final int SLOT_MINUTES = 30;
    public static final int MAX_PATIENTS_PER_DOCTOR_PER_PERIOD = 8;
    public static final int MAX_PATIENTS_PER_DOCTOR_PER_DAY = 16;
    public static final int MAX_ACTIVE_FUTURE_APPOINTMENTS_PER_PATIENT = 5;
    public static final int MAX_ADVANCE_DAYS = 90;

    private AppointmentRules() {}

    public static LocalDateTime nowInVietnam() {
        return LocalDateTime.now(VIETNAM_ZONE);
    }

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
        if (!time.isBefore(MORNING_END) && time.isBefore(AFTERNOON_START)) {
            throw new IllegalArgumentException("Phòng khám nghỉ trưa từ 12:00 đến 13:00.");
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
                ? time.isBefore(MORNING_END)
                : !time.isBefore(AFTERNOON_START);
        if (!matches) {
            throw new IllegalArgumentException("Giờ khám không thuộc buổi bệnh nhân đã chọn.");
        }
    }

    public static void validateCheckInDate(LocalDateTime appointmentAt, LocalDate today) {
        if (appointmentAt == null || today == null || !appointmentAt.toLocalDate().equals(today)) {
            throw new IllegalArgumentException("Chỉ được ghi nhận đến khám trong ngày hẹn.");
        }
    }

    public static String periodOf(LocalTime time) {
        if (time != null && time.isBefore(MORNING_END)) return "MORNING";
        if (time != null && !time.isBefore(AFTERNOON_START)) return "AFTERNOON";
        throw new IllegalArgumentException("Không thể xếp lịch trong giờ nghỉ trưa.");
    }

    public static void validateCapacity(long doctorSlot, long patientDay,
            long doctorPeriod, long doctorDay) {
        if (doctorSlot > 0) throw new IllegalArgumentException("Bác sĩ đã có bệnh nhân trong khung giờ này.");
        if (patientDay > 0) throw new IllegalArgumentException("Bệnh nhân đã có một lịch đang xử lý trong ngày này.");
        if (doctorPeriod >= MAX_PATIENTS_PER_DOCTOR_PER_PERIOD)
            throw new IllegalArgumentException("Bác sĩ đã đủ 8 bệnh nhân trong buổi này.");
        if (doctorDay >= MAX_PATIENTS_PER_DOCTOR_PER_DAY)
            throw new IllegalArgumentException("Bác sĩ đã đủ 16 bệnh nhân trong ngày này.");
    }

    public static void validatePatientRequestCapacity(long sameDay, long activeFuture) {
        if (sameDay > 0) throw new IllegalArgumentException("Bạn đã có một lịch đang xử lý trong ngày này.");
        if (activeFuture >= MAX_ACTIVE_FUTURE_APPOINTMENTS_PER_PATIENT)
            throw new IllegalArgumentException("Bạn chỉ được có tối đa 5 lịch khám sắp tới.");
    }
}
