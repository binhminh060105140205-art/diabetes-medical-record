package controllers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import models.PatientDailyLog;
import org.junit.jupiter.api.Test;

class PatientHealthControllerTest {

    @Test
    void rejectsBloodPressureWhenOnlyOneValueIsProvided() {
        PatientHealthController controller = new PatientHealthController();
        PatientDailyLog log = new PatientDailyLog();
        log.setSystolicBp(120);

        assertThrows(IllegalArgumentException.class, () -> controller.validate(log));
    }

    @Test
    void acceptsBloodPressureWhenBothValuesAreProvided() {
        PatientHealthController controller = new PatientHealthController();
        PatientDailyLog log = new PatientDailyLog();
        log.setSystolicBp(120);
        log.setDiastolicBp(80);

        assertDoesNotThrow(() -> controller.validate(log));
    }
}
