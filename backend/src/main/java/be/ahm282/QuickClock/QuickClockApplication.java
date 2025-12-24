package be.ahm282.QuickClock;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@SpringBootApplication
public class QuickClockApplication {

	@Value("${app.localization.timezone:Africa/Cairo}")
	private String applicationTimezone;

	@PostConstruct
	public void init() {
		// Set the default timezone for the entire application
		TimeZone.setDefault(TimeZone.getTimeZone(applicationTimezone));
		System.out.println("Application timezone set to: " + applicationTimezone);
	}

	public static void main(String[] args) {
		SpringApplication.run(QuickClockApplication.class, args);
	}

}
