package be.ahm282.QuickClock.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                // Localhost
                "https://localhost",
                "http://localhost",
                "https://127.0.0.1",
                "http://127.0.0.1",

                // Dev
                "http://localhost:8081",
                "https://localhost:8081",
                "http://localhost:5173",
                "https://localhost:5173",
                "http://frontend:8081",
                "http://192.168.0.62:8081",
                "https://192.168.0.62:8081",

                // Production
                "http://quickclock.dev",
                "https://quickclock.dev",

                // LAN
                "http://192.168.0.62",
                "https://192.168.0.62",
                "http://192.168.0.62:5173",
                "https://192.168.0.62:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-QuickClock-Guard"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type", "X-QuickClock-Guard"));
        config.setAllowCredentials(true);
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}