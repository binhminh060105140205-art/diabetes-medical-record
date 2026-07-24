package controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import models.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ControllerSupportTest {

    @Test
    void clinicWorkflowDefaultsToEncountersWhenViewIsMissing() {
        ClinicWorkflowController controller = new ClinicWorkflowController();

        assertEquals("encounters", controller.normalizeView(null, "DOCTOR"));
        assertEquals("encounters", controller.normalizeView("unknown", "DOCTOR"));
        assertEquals("encounters", controller.normalizeView("appointments", "DOCTOR"));
        assertEquals("encounters", controller.normalizeView("clinical", "STAFF"));
        assertEquals("labs", controller.normalizeView("labs", "DOCTOR"));
    }

    @Test
    void adminDashboardNormalizesFiltersAndSortOrder() {
        assertEquals("ACTIVE", AdminDashboardController.normalizeStatus("active"));
        assertEquals("INACTIVE", AdminDashboardController.normalizeStatus("INACTIVE"));
        assertEquals("", AdminDashboardController.normalizeStatus("locked"));
        assertEquals("DOCTOR", AdminDashboardController.normalizeRole("doctor"));
        assertEquals("", AdminDashboardController.normalizeRole("unknown"));
        assertEquals("OLDEST", AdminDashboardController.normalizeSort("oldest"));
        assertEquals("NEWEST", AdminDashboardController.normalizeSort(null));
    }
    @Test
    void normalizesTextAndPositiveIds() {
        assertEquals("value", ControllerSupport.clean("  value  "));
        assertEquals("", ControllerSupport.clean(null));
        assertEquals(12, ControllerSupport.positiveId("12", "ID"));
        assertEquals(0, ControllerSupport.positiveIdOrZero("invalid"));
        assertThrows(IllegalArgumentException.class,
                () -> ControllerSupport.positiveId("0", "ID"));
    }

    @Test
    void checksOnlyAllowedRoles() {
        User user = new User();
        user.setRole("DOCTOR");

        assertTrue(ControllerSupport.hasRole(user, "STAFF", "DOCTOR"));
        assertFalse(ControllerSupport.hasRole(user, "PATIENT"));
        assertFalse(ControllerSupport.hasRole(null, "DOCTOR"));
    }

    @Test
    void combinesVietnameseAppointmentDateAndTimeFields() {
        assertEquals(LocalDateTime.of(2026, 7, 21, 14, 30),
                ControllerSupport.appointmentDateTime("2026-07-21", "14:30"));
        assertEquals(LocalDateTime.of(2026, 7, 21, 14, 30),
                ControllerSupport.appointmentDateTime("2026-07-21T14:30"));
        assertThrows(IllegalArgumentException.class,
                () -> ControllerSupport.appointmentDateTime("2026-07-21", "14:22"));
        assertThrows(IllegalArgumentException.class,
                () -> ControllerSupport.appointmentDateTime("2026-07-21T14:22"));
        assertThrows(IllegalArgumentException.class,
                () -> ControllerSupport.appointmentDateTime("", "14:30"));
    }

    @Test
    void roundsDatetimePickerMinimumToNextValidClinicSlot() {
        assertEquals(LocalDateTime.of(2026, 7, 21, 9, 0),
                ControllerSupport.nextAppointmentSlot(
                        LocalDateTime.of(2026, 7, 21, 8, 31)));
        assertEquals(LocalDateTime.of(2026, 7, 21, 13, 0),
                ControllerSupport.nextAppointmentSlot(
                        LocalDateTime.of(2026, 7, 21, 11, 50)));
        assertEquals(LocalDateTime.of(2026, 7, 27, 7, 30),
                ControllerSupport.nextAppointmentSlot(
                        LocalDateTime.of(2026, 7, 25, 17, 1)));
    }

    @Test
    void exposesOnlyValidClinicTimeSlots() {
        var slots = ControllerSupport.appointmentTimeOptions();
        assertTrue(slots.stream().anyMatch(slot -> "07:30".equals(slot.get("value"))));
        assertTrue(slots.stream().anyMatch(slot -> "17:00".equals(slot.get("value"))));
        assertFalse(slots.stream().anyMatch(slot -> "12:00".equals(slot.get("value"))));
        assertFalse(slots.stream().anyMatch(slot -> "14:22".equals(slot.get("value"))));
    }
}
