package vn.diabetes.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AppointmentRulesTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 16, 8, 0);

    @Test
    void acceptsValidHalfHourSlot() {
        assertDoesNotThrow(() -> AppointmentRules.validate(
                LocalDateTime.of(2026, 7, 17, 9, 30), now));
    }

    @Test
    void rejectsSunday() {
        assertThrows(IllegalArgumentException.class, () -> AppointmentRules.validate(
                LocalDateTime.of(2026, 7, 19, 9, 0), now));
    }

    @Test
    void rejectsOutsideOpeningHours() {
        assertThrows(IllegalArgumentException.class, () -> AppointmentRules.validate(
                LocalDateTime.of(2026, 7, 17, 7, 0), now));
        assertThrows(IllegalArgumentException.class, () -> AppointmentRules.validate(
                LocalDateTime.of(2026, 7, 17, 17, 30), now));
        assertThrows(IllegalArgumentException.class, () -> AppointmentRules.validate(
                LocalDateTime.of(2026, 7, 17, 12, 0), now));
        assertThrows(IllegalArgumentException.class, () -> AppointmentRules.validate(
                LocalDateTime.of(2026, 7, 17, 12, 30), now));
    }

    @Test
    void rejectsNonHalfHourSlot() {
        assertThrows(IllegalArgumentException.class, () -> AppointmentRules.validate(
                LocalDateTime.of(2026, 7, 17, 9, 15), now));
    }

    @Test
    void rejectsAppointmentsMoreThanNinetyDaysAhead() {
        assertThrows(IllegalArgumentException.class, () -> AppointmentRules.validate(
                now.plusDays(91).withHour(9).withMinute(0), now));
    }

    @Test
    void acceptsSimpleDateRequestAndRejectsSunday() {
        assertDoesNotThrow(() -> AppointmentRules.validateRequestedDate(
                now.toLocalDate().plusDays(1), now.toLocalDate()));
        assertThrows(IllegalArgumentException.class, () -> AppointmentRules.validateRequestedDate(
                java.time.LocalDate.of(2026, 7, 19), now.toLocalDate()));
    }

    @Test
    void assignmentMustMatchRequestedDateAndPeriod() {
        assertDoesNotThrow(() -> AppointmentRules.validateAssignmentMatchesRequest(
                LocalDateTime.of(2026, 7, 17, 9, 30),
                java.time.LocalDate.of(2026, 7, 17), "MORNING"));
        assertThrows(IllegalArgumentException.class, () -> AppointmentRules.validateAssignmentMatchesRequest(
                LocalDateTime.of(2026, 7, 17, 14, 0),
                java.time.LocalDate.of(2026, 7, 17), "MORNING"));
    }

    @Test
    void rejectsDoctorAndPatientCapacityOverflow() {
        assertDoesNotThrow(() -> AppointmentRules.validateCapacity(0, 0, 7, 15));
        assertThrows(IllegalArgumentException.class,
                () -> AppointmentRules.validateCapacity(1, 0, 7, 15));
        assertThrows(IllegalArgumentException.class,
                () -> AppointmentRules.validateCapacity(0, 1, 7, 15));
        assertThrows(IllegalArgumentException.class,
                () -> AppointmentRules.validateCapacity(0, 0, 8, 15));
        assertThrows(IllegalArgumentException.class,
                () -> AppointmentRules.validateCapacity(0, 0, 7, 16));
    }

    @Test
    void limitsFutureAppointmentsPerPatient() {
        assertDoesNotThrow(() -> AppointmentRules.validatePatientRequestCapacity(0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> AppointmentRules.validatePatientRequestCapacity(1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> AppointmentRules.validatePatientRequestCapacity(0, 2));
    }
}
