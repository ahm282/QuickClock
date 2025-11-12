package be.ahm282.QuickClock.infrastructure.config;

import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.application.services.ClockService;
import be.ahm282.QuickClock.domain.exception.BusinessRuleException;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class BusinessRuleExceptionTest {
    @Test
    void shouldThrowWhenClockingOutTwice() {
        ClockRecordRepositoryPort repo = Mockito.mock(ClockRecordRepositoryPort.class);
        List<ClockRecord> history = List.of(new ClockRecord(1L, 1L, "OUT", LocalDateTime.now()));
        Mockito.when(repo.findAllByUserId(1L)).thenReturn(history);

        ClockService service = new ClockService(repo);

        assertThrows(BusinessRuleException.class, () -> service.clockOut(1L));
    }
}
