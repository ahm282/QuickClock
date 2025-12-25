# QuickClock Backend

Spring Boot 4 REST API for QuickClock employee time tracking system.

## Quick Start

```bash
# 1. Setup (first time only)
./setup-backend.sh

# 2. Run
./gradlew bootRun

# Access at: https://localhost:8081
```

## Running in IntelliJ IDEA

### First Time Setup

1. **Open Project**

    - File → Open → Select this `backend` folder
    - Wait for Gradle sync to complete

2. **Install EnvFile Plugin** (Recommended)

    - File → Settings → Plugins
    - Search "EnvFile" → Install → Restart

3. **Configure Environment**

    ```bash
    cp .env.example .env
    nano .env  # Edit with your values
    ```

4. **Generate SSL Certificate**

    ```bash
    ./generate-local-ssl.sh
    ```

5. **Run Configuration**
    - A run configuration "QuickClock Backend (Dev)" should appear
    - If using EnvFile plugin:
        - Edit configuration → EnvFile tab
        - Add `.env` file
    - Click Run

### Without EnvFile Plugin

Manually set environment variables in run configuration:

-   Run → Edit Configurations
-   Add all variables from `.env` file manually

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/be/ahm282/QuickClock/
│   │   │   ├── application/        # Application layer (use cases)
│   │   │   ├── domain/             # Domain models & interfaces
│   │   │   ├── infrastructure/     # Infrastructure (DB, security, etc.)
│   │   │   └── presentation/       # REST controllers
│   │   └── resources/
│   │       ├── application.yml     # Main configuration
│   │       ├── application-dev.yml # Development profile
│   │       ├── application-prod.yml # Production profile
│   │       ├── keystore.p12       # SSL certificate (git ignored)
│   │       └── db/migration/      # Database migrations
│   └── test/                       # Unit & integration tests
├── .env                            # Environment variables (git ignored)
├── .env.example                    # Environment template
├── build.gradle                    # Project dependencies
└── gradlew                         # Gradle wrapper
```

## Environment Variables

See `.env.example` for all available variables.

**Required:**

-   `JWT_SECRET` - JWT signing key (generate with `openssl rand -base64 64`)
-   `SSL_KEYSTORE_PASSWORD` - Password for keystore.p12
-   `SUPER_ADMIN_USERNAME` - Initial admin username
-   `SUPER_ADMIN_PASSWORD` - Initial admin password
-   `KIOSK_USERNAME` - Kiosk account username
-   `KIOSK_PASSWORD` - Kiosk account password

## Development

### Run Tests

```bash
./gradlew test
```

### Build JAR

```bash
./gradlew bootJar

# JAR location: build/libs/QuickClock-0.0.1-SNAPSHOT.jar
```

### Run JAR

```bash
java -jar build/libs/QuickClock-0.0.1-SNAPSHOT.jar
```

### Access H2 Database Console

-   URL: https://localhost:8081/h2-console
-   JDBC URL: `jdbc:h2:mem:quickclock`
-   Username: `sa`
-   Password: (empty)

### API Documentation

Once running, access:

-   Health: https://localhost:8081/actuator/health
-   Endpoints: https://localhost:8081/api/\*

## Profiles

### dev (default)

-   H2 in-memory database
-   SQL logging enabled
-   Auto-creates sample data
-   HTTPS with self-signed certificate

### prod

-   PostgreSQL database
-   Optimized logging
-   No sample data
-   Requires real SSL certificate

## Technology Stack

-   **Java 21**
-   **Spring Boot 4.0.0**
-   **Spring Security** - Authentication & authorization
-   **Spring Data JPA** - Database access
-   **H2 Database** - Development database
-   **PostgreSQL** - Production database
-   **JWT (JJWT)** - Token-based authentication
-   **Gradle** - Build tool
-   **JUnit 5** - Testing

## Troubleshooting

### Port 8081 already in use

```bash
# Find process
sudo lsof -i :8081

# Kill it
kill -9 <PID>
```

### SSL Certificate Error

```bash
# Regenerate certificate
rm src/main/resources/keystore.p12
./generate-local-ssl.sh
```

### JWT_SECRET not found

-   Ensure `.env` file exists
-   Check IntelliJ is loading environment variables
-   Verify EnvFile plugin is configured correctly

### Database not seeding

-   Check `SEED_ENABLED=true` in `.env`
-   Verify admin credentials are set
-   Check logs for errors

## License

[Your License Here]
