package be.ahm282.QuickClock.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.DayOfWeek;
import java.time.ZoneId;

@Configuration
@ConfigurationProperties(prefix = "app.localization")
public class LocalizationConfig {

    private String timezone = "Africa/Cairo";
    private DayOfWeek weekStartDay = DayOfWeek.SATURDAY;

    public ZoneId getZoneId() {
        return ZoneId.of(timezone);
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public DayOfWeek getWeekStartDay() {
        return weekStartDay;
    }

    public void setWeekStartDay(String weekStartDay) {
        this.weekStartDay = DayOfWeek.valueOf(weekStartDay.toUpperCase());
    }

    public void setWeekStartDay(DayOfWeek weekStartDay) {
        this.weekStartDay = weekStartDay;
    }
}

