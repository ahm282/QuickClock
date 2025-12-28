package be.ahm282.QuickClock.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        // Returns the system default time zone.
        return Clock.systemDefaultZone();
    }
}