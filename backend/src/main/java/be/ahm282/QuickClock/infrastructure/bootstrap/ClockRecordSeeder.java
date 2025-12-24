package be.ahm282.QuickClock.infrastructure.bootstrap;

import be.ahm282.QuickClock.application.ports.out.ClockRecordRepositoryPort;
import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.model.ClockRecord;
import be.ahm282.QuickClock.domain.model.ClockRecordType;
import be.ahm282.QuickClock.domain.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Seeds realistic clock records for development and testing.
 * Creates clock-in/out records for the current week to test work hours functionality.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(2) // Run after UserSeeder
@DependsOn("userSeeder")
public class ClockRecordSeeder {

    private final ClockRecordRepositoryPort clockRecordRepository;
    private final UserRepositoryPort userRepository;
    private final Random random;
    private final ZoneId cairoZone = ZoneId.of("Africa/Cairo");

    public ClockRecordSeeder(ClockRecordRepositoryPort clockRecordRepository,
                             UserRepositoryPort userRepository) {
        this.clockRecordRepository = clockRecordRepository;
        this.userRepository = userRepository;
        this.random = new Random(42); // Fixed seed for reproducibility
    }

    @PostConstruct
    public void seed() {
        System.out.println("🕐 Starting clock record seeding...");

        // Get all users to seed records for
        String[] usernames = {
                "bob", "alice", "mohamed.salah", "nour.ibrahim",
                "omar.khaled", "layla.mahmoud", "youssef.ahmed",
                "sarah.mostafa", "karim.hassan", "mariam.said"
        };

        LocalDate today = LocalDate.now(cairoZone);
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));

        int totalRecords = 0;
        for (String username : usernames) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                continue;
            }

            User user = userOpt.get();
            totalRecords += seedUserClockRecords(user, startOfWeek, today);
        }

        System.out.println("✅ Seeded " + totalRecords + " clock records for " + usernames.length + " employees");
    }

    /**
     * Seed clock records for a user from start of week until today.
     * Creates realistic work patterns (8-9 hours per day, with lunch breaks).
     */
    private int seedUserClockRecords(User user, LocalDate startOfWeek, LocalDate today) {
        List<ClockRecord> records = new ArrayList<>();
        LocalDate currentDate = startOfWeek;

        while (!currentDate.isAfter(today)) {
            // Skip some days randomly (sick days, vacations)
            if (random.nextDouble() > 0.85) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            // Skip if it's Friday (weekend in Egypt for some companies)
            if (currentDate.getDayOfWeek() == DayOfWeek.FRIDAY) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            // Morning session
            int morningStartHour = 8 + random.nextInt(2); // 8-9 AM
            int morningStartMinute = random.nextInt(30); // 0-29 minutes
            LocalTime morningStart = LocalTime.of(morningStartHour, morningStartMinute);

            int lunchHour = 12 + random.nextInt(2); // 12-1 PM
            LocalTime lunchTime = LocalTime.of(lunchHour, random.nextInt(30));

            Instant morningIn = currentDate.atTime(morningStart).atZone(cairoZone).toInstant();
            Instant morningOut = currentDate.atTime(lunchTime).atZone(cairoZone).toInstant();

            records.add(ClockRecord.createAt(user.getId(), ClockRecordType.IN, morningIn, null));
            records.add(ClockRecord.createAt(user.getId(), ClockRecordType.OUT, morningOut, null));

            // Afternoon session
            int afternoonStartHour = lunchHour + 1; // After lunch
            LocalTime afternoonStart = LocalTime.of(afternoonStartHour, random.nextInt(15));

            int endHour = 16 + random.nextInt(3); // 4-6 PM
            LocalTime endTime = LocalTime.of(endHour, random.nextInt(60));

            Instant afternoonIn = currentDate.atTime(afternoonStart).atZone(cairoZone).toInstant();
            Instant afternoonOut = currentDate.atTime(endTime).atZone(cairoZone).toInstant();

            records.add(ClockRecord.createAt(user.getId(), ClockRecordType.IN, afternoonIn, null));

            // If today and random chance, leave them clocked in (simulate ongoing work)
            if (currentDate.equals(today) && random.nextDouble() > 0.5) {
                // Currently clocked in - don't add OUT record
                System.out.println("  📍 " + user.getDisplayName() + " is currently clocked in");
            } else {
                records.add(ClockRecord.createAt(user.getId(), ClockRecordType.OUT, afternoonOut, null));
            }

            currentDate = currentDate.plusDays(1);
        }

        // Save all records
        records.forEach(clockRecordRepository::save);
        return records.size();
    }
}

