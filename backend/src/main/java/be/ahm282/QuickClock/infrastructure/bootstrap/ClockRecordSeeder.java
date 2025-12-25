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

        // Use Cairo time consistently
        ZonedDateTime nowZdt = ZonedDateTime.now(cairoZone).withSecond(0).withNano(0);
        Instant now = nowZdt.toInstant();

        LocalDate today = nowZdt.toLocalDate();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));

        String[] usernames = {
                "bob", "alice", "mohamed.salah", "nour.ibrahim",
                "omar.khaled", "layla.mahmoud", "youssef.ahmed",
                "sarah.mostafa", "karim.hassan", "mariam.said"
        };

        int totalRecords = 0;
        for (String username : usernames) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) continue;

            User user = userOpt.get();
            totalRecords += seedUserClockRecords(user, startOfWeek, today, now);
        }

        System.out.println("✅ Seeded " + totalRecords + " clock records for " + usernames.length + " employees");
    }

    /**
     * Seed clock records for a user from start of week until today.
     * Creates realistic work patterns with lunch breaks.
     * Ensures no record is created in the future relative to 'now'.
     * Does NOT skip Friday (shop is open).
     */
    private int seedUserClockRecords(User user, LocalDate startOfWeek, LocalDate today, Instant now) {
        List<ClockRecord> records = new ArrayList<>();
        LocalDate currentDate = startOfWeek;

        while (!currentDate.isAfter(today)) {
            // Skip some days randomly (sick days, vacations)
            if (random.nextDouble() > 0.85) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            // Generate a plausible schedule for this day
            int morningStartHour = 8 + random.nextInt(2);          // 08–09
            int morningStartMinute = random.nextInt(30);           // 00–29
            LocalTime morningStart = LocalTime.of(morningStartHour, morningStartMinute);

            int lunchHour = 12 + random.nextInt(2);                // 12–13
            LocalTime lunchTime = LocalTime.of(lunchHour, random.nextInt(30));

            int afternoonStartHour = lunchHour + 1;                // after lunch
            LocalTime afternoonStart = LocalTime.of(afternoonStartHour, random.nextInt(15));

            int endHour = 16 + random.nextInt(3);                  // 16–18
            LocalTime endTime = LocalTime.of(endHour, random.nextInt(60));

            Instant morningIn = currentDate.atTime(morningStart).atZone(cairoZone).toInstant();
            Instant morningOut = currentDate.atTime(lunchTime).atZone(cairoZone).toInstant();
            Instant afternoonIn = currentDate.atTime(afternoonStart).atZone(cairoZone).toInstant();
            Instant afternoonOut = currentDate.atTime(endTime).atZone(cairoZone).toInstant();

            // -------- TODAY SPECIAL CASE --------
            // If today's first planned IN is still in the future, fabricate a small "already working" session
            // relative to NOW so hoursToday is not 0.
            if (currentDate.equals(today) && morningIn.isAfter(now)) {
                ZonedDateTime nowCairo = now.atZone(cairoZone);
                ZonedDateTime startOfToday = today.atStartOfDay(cairoZone);

                int minutesAgo = 45 + random.nextInt(76); // 45..120
                ZonedDateTime inZdt = nowCairo.minusMinutes(minutesAgo).withSecond(0).withNano(0);
                if (inZdt.isBefore(startOfToday)) {
                    inZdt = startOfToday.plusMinutes(5);
                }

                Instant in = inZdt.toInstant();
                records.add(ClockRecord.createAt(user.getId(), ClockRecordType.IN, in, null));

                // 50%: keep them clocked in; else close it a few minutes before now
                if (random.nextDouble() > 0.5) {
                    System.out.println("  📍 " + user.getDisplayName() + " is currently clocked in (seeded early)");
                } else {
                    Instant out = nowCairo.minusMinutes(5).withSecond(0).withNano(0).toInstant();
                    if (out.isAfter(in)) {
                        records.add(ClockRecord.createAt(user.getId(), ClockRecordType.OUT, out, null));
                    }
                }

                currentDate = currentDate.plusDays(1);
                continue;
            }

            // -------- NORMAL DAY (AND TODAY WITHIN/WITHOUT FUTURE) --------

            // Morning IN if it is not in the future
            if (!morningIn.isAfter(now)) {
                records.add(ClockRecord.createAt(user.getId(), ClockRecordType.IN, morningIn, null));
            } else {
                // Should not happen due to today's special case, but safe.
                currentDate = currentDate.plusDays(1);
                continue;
            }

            // Morning OUT only if it's not in the future; otherwise keep user clocked in and stop for today
            if (!morningOut.isAfter(now) && morningOut.isAfter(morningIn)) {
                records.add(ClockRecord.createAt(user.getId(), ClockRecordType.OUT, morningOut, null));
            } else {
                // Still morning session (or bad schedule) -> stop. WorkHoursService will count IN to now.
                if (currentDate.equals(today)) {
                    System.out.println("  📍 " + user.getDisplayName() + " is currently clocked in (morning)");
                }
                currentDate = currentDate.plusDays(1);
                continue;
            }

            // Afternoon IN only if it's not in the future; if it's in the future, stop (user is on break)
            if (afternoonIn.isAfter(now)) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            records.add(ClockRecord.createAt(user.getId(), ClockRecordType.IN, afternoonIn, null));

            // If today and random chance, leave them clocked in (only valid if afternoonIn <= now)
            if (currentDate.equals(today) && random.nextDouble() > 0.5) {
                System.out.println("  📍 " + user.getDisplayName() + " is currently clocked in");
                currentDate = currentDate.plusDays(1);
                continue;
            }

            // Afternoon OUT must not be in the future; clamp to now for today
            Instant out = afternoonOut.isAfter(now) ? now : afternoonOut;
            if (out.isAfter(afternoonIn)) {
                records.add(ClockRecord.createAt(user.getId(), ClockRecordType.OUT, out, null));
            } else {
                // If clamping makes it invalid, treat as still clocked in
                if (currentDate.equals(today)) {
                    System.out.println("  📍 " + user.getDisplayName() + " is currently clocked in (afternoon)");
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        records.forEach(clockRecordRepository::save);
        return records.size();
    }
}

