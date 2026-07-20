package controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import models.User;
import org.junit.jupiter.api.Test;

class ControllerSupportTest {
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
}
