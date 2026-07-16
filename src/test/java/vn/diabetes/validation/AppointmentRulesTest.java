package vn.diabetes.validation;
import static org.junit.jupiter.api.Assertions.*;import java.time.*;import org.junit.jupiter.api.Test;
class AppointmentRulesTest {
 private final LocalDateTime now=LocalDateTime.of(2026,7,16,8,0);
 @Test void acceptsValidHalfHourSlot(){assertDoesNotThrow(()->AppointmentRules.validate(LocalDateTime.of(2026,7,17,9,30),now));}
 @Test void rejectsSunday(){assertThrows(IllegalArgumentException.class,()->AppointmentRules.validate(LocalDateTime.of(2026,7,19,9,0),now));}
 @Test void rejectsOutsideOpeningHours(){assertThrows(IllegalArgumentException.class,()->AppointmentRules.validate(LocalDateTime.of(2026,7,17,7,0),now));assertThrows(IllegalArgumentException.class,()->AppointmentRules.validate(LocalDateTime.of(2026,7,17,17,30),now));}
 @Test void rejectsNonHalfHourSlot(){assertThrows(IllegalArgumentException.class,()->AppointmentRules.validate(LocalDateTime.of(2026,7,17,9,15),now));}
}
