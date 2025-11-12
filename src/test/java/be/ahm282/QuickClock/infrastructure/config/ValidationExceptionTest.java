package be.ahm282.QuickClock.infrastructure.config;

import be.ahm282.QuickClock.domain.exception.ValidationException;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidationExceptionTest {

    @Test
    public void shouldThrowWhenTypeInvalid() {
        assertThrows(ValidationException.class, () -> ClockRecord.create(1L, "INVALID"));
    }

    @Test
    void shouldThrowWhenUserIdNull() {
        assertThrows(ValidationException.class, () -> ClockRecord.create(null, "IN"));
    }

    @Test
    void shouldThrowWhenTimestampMissing() {
        assertThrows(ValidationException.class, () -> new ClockRecord(1L, 1L, "IN", null));
    }
}
