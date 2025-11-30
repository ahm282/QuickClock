package be.ahm282.QuickClock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class QuickClockApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuickClockApplication.class, args);
	}

}
