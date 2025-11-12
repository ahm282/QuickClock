package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

public class ClockServiceTest {

    @Test
    public void clockInCreatesRecord() {
        ClockRecordRepositoryPort recordRepository = Mockito.mock(ClockRecordRepositoryPort.class);
        ClockRecord savedClockRecord = new ClockRecord();
        savedClockRecord.setId(42L);
        Mockito.when(recordRepository.save(Mockito.any())).thenAnswer(inv -> {
            ClockRecord clockRecord = inv.getArgument(0);
            clockRecord.setId(42L);
            return clockRecord;
        });

        ClockService clockService = new ClockService(recordRepository);
        ClockRecord clockRecord = clockService.clockIn(42L);

        assertThat(clockRecord.getId()).isNotNull();
        assertThat(clockRecord.getId()).isEqualTo(42L);
        assertThat(clockRecord.getType()).isEqualTo("IN");
    }
}
