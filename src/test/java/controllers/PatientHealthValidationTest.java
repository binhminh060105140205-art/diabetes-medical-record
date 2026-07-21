package controllers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import models.PatientDailyLog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Verifies the server-side rules used by the patient daily-health form. */
class PatientHealthValidationTest {
    private static Method validateMethod;

    @BeforeAll
    static void exposeValidationMethod() throws NoSuchMethodException {
        validateMethod = PatientHealthController.class
                .getDeclaredMethod("validate", PatientDailyLog.class);
        validateMethod.setAccessible(true);
    }

    @Test
    void acceptsValidDailyHealthLog() {
        PatientDailyLog log = new PatientDailyLog();
        log.setBloodGlucose(105.0);
        log.setSystolicBp(120);
        log.setDiastolicBp(80);
        log.setWeight(65.0);
        log.setMealType("FASTING");
        log.setNote("Theo dõi định kỳ");

        assertDoesNotThrow(() -> validate(log));
    }

    @Test
    void rejectsMealTimeWithoutBloodGlucose() {
        PatientDailyLog log = new PatientDailyLog();
        log.setMealType("FASTING");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validate(log));
        assertEquals("Đã chọn thời điểm đo thì cần nhập đường huyết.", error.getMessage());
    }

    @Test
    void rejectsEmptyDailyLog() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validate(new PatientDailyLog()));
        assertEquals("Cần nhập ít nhất một chỉ số hoặc ghi chú.", error.getMessage());
    }

    @Test
    void rejectsDiastolicPressureGreaterThanSystolicPressure() {
        PatientDailyLog log = new PatientDailyLog();
        log.setSystolicBp(80);
        log.setDiastolicBp(120);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validate(log));
        assertEquals("Huyết áp tâm thu phải lớn hơn tâm trương.", error.getMessage());
    }

    private void validate(PatientDailyLog log) {
        try {
            validateMethod.invoke(new PatientHealthController(), log);
        } catch (IllegalAccessException error) {
            throw new AssertionError("Không thể gọi bộ validate sức khỏe bệnh nhân", error);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new AssertionError("Bộ validate phát sinh lỗi ngoài dự kiến", cause);
        }
    }
}
