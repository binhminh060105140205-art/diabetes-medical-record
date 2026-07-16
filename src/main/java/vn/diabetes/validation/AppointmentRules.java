package vn.diabetes.validation;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class AppointmentRules {
    public static final LocalTime OPEN_TIME = LocalTime.of(7, 30);
    public static final LocalTime CLOSE_TIME = LocalTime.of(17, 30);
    public static final int SLOT_MINUTES = 30;
    public static final int MAX_PATIENTS_PER_DOCTOR_PER_DAY = 20;

    private AppointmentRules() {}

    public static void validate(LocalDateTime appointmentAt, LocalDateTime now) {
        if (appointmentAt == null || !appointmentAt.isAfter(now.plusMinutes(15))) {
            throw new IllegalArgumentException("Lịch hẹn phải sau hiện tại ít nhất 15 phút.");
        }
        if (appointmentAt.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Phòng khám nghỉ Chủ nhật.");
        }
        LocalTime time = appointmentAt.toLocalTime();
        if (time.isBefore(OPEN_TIME) || time.isAfter(CLOSE_TIME.minusMinutes(SLOT_MINUTES))) {
            throw new IllegalArgumentException("Chỉ nhận lịch từ 07:30 đến 17:00.");
        }
        if (time.getMinute() % SLOT_MINUTES != 0 || time.getSecond() != 0 || time.getNano() != 0) {
            throw new IllegalArgumentException("Giờ hẹn phải theo khung 30 phút.");
        }
    }
}
