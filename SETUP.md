# QuickClock - Fresh Setup Guide

## Prerequisites

-   **Java 21** (JDK 21)
-   **IntelliJ IDEA** (Community or Ultimate)
-   **Git**
-   **OpenSSL** (for certificate generation)
-   **Keytool** (comes with JDK)

## Setup Instructions (Fresh Clone)

### 1. Clone the Repository

```bash
git clone <your-repo-url>
cd QuickClock
```

### 2. Configure Backend Environment

```bash
# Navigate to backend directory
cd backend

# Copy environment template
cp .env.example .env

# Edit .env and set your values
nano .env  # or use your preferred editor
```

**Required values in `backend/.env`:**

```bash
# Generate JWT secret
JWT_SECRET=$(openssl rand -base64 64)

# Set a keystore password (remember this!)
SSL_KEYSTORE_PASSWORD=YourSecurePassword123

# Set admin credentials
SUPER_ADMIN_USERNAME=superadmin
SUPER_ADMIN_PASSWORD=YourAdminPassword123!
SUPER_ADMIN_DISPLAYNAME=Your Name

# Set kiosk credentials
KIOSK_USERNAME=kiosk
KIOSK_PASSWORD=YourKioskPassword123!
```

### 3. Generate SSL Certificates

The backend requires HTTPS certificates for development.

```bash
# From backend directory
cd src/main/resources

# Generate keystore.p12 (use the password from your .env file)
keytool -genkeypair \
  -alias springboot \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore keystore.p12 \
  -validity 365 \
  -storepass "YourSecurePassword123" \
  -dname "CN=localhost, OU=QuickClock, O=Development, L=Local, ST=Dev, C=US" \
  -ext "SAN=DNS:localhost,DNS:backend,IP:127.0.0.1"

# Verify it was created
ls -l keystore.p12
```

**Important:** The `-storepass` value MUST match `SSL_KEYSTORE_PASSWORD` in your `.env` file!

### 4. Open Project in IntelliJ IDEA

#### Import the Project

1. Open IntelliJ IDEA
2. Choose **File → Open**
3. Navigate to `QuickClock/backend` folder
4. Select the `backend` folder and click **Open**
5. IntelliJ will detect it's a Gradle project and import automatically

#### Wait for Gradle Sync

-   IntelliJ will download dependencies automatically
-   This may take a few minutes on first run
-   Watch the progress in the bottom right corner

### 5. Configure IntelliJ Run Configuration

#### Option A: Use EnvFile Plugin (Recommended)

1. Install **EnvFile** plugin:

    - Go to **File → Settings → Plugins**
    - Search for "EnvFile"
    - Install and restart IntelliJ

2. Create Run Configuration:
    - Click **Run → Edit Configurations**
    - Click **+** → **Spring Boot**
    - Name: `QuickClock Backend (Dev)`
    - Module: `QuickClock.main`
    - Main class: `be.ahm282.QuickClock.QuickClockApplication`
    - Active profiles: `dev`
    - Go to **EnvFile** tab
    - Click **+** → Select `backend/.env`
    - Click **Apply** and **OK**

#### Option B: Manual Environment Variables

If you don't want to use the plugin:

1. Create Run Configuration:
    - Click **Run → Edit Configurations**
    - Click **+** → **Spring Boot**
    - Name: `QuickClock Backend (Dev)`
    - Module: `QuickClock.main`
    - Main class: `be.ahm282.QuickClock.QuickClockApplication`
    - Active profiles: `dev`
2. Add Environment Variables:
    - Click on **Modify options** → Check **Environment variables**
    - Click the folder icon next to Environment variables
    - Manually add each variable from your `.env` file:
        ```
        JWT_SECRET=your_value_here
        SSL_KEYSTORE_PASSWORD=your_value_here
        SUPER_ADMIN_USERNAME=superadmin
        SUPER_ADMIN_PASSWORD=your_password_here
        SUPER_ADMIN_DISPLAYNAME=Your Name
        KIOSK_USERNAME=kiosk
        KIOSK_PASSWORD=your_password_here
        # ... add all others
        ```

### 6. Run the Backend

1. Select your run configuration from the dropdown (top right)
2. Click the **Run** button (green triangle) or press **Shift+F10**
3. Backend should start on **https://localhost:8081**

### 7. Test the Backend

```bash
# Health check
curl -k https://localhost:8081/actuator/health

# Should return: {"status":"UP"}
```

### 8. Access H2 Console (Optional)

-   URL: https://localhost:8081/h2-console
-   JDBC URL: `jdbc:h2:mem:quickclock`
-   Username: `sa`
-   Password: (leave empty)

---

## Frontend Setup (Optional)

### 1. Install Dependencies

```bash
cd ../QuickClock-UI
npm install
```

### 2. Run Development Server

```bash
npm start
```

-   Frontend runs on **https://localhost:5173**
-   Will proxy API requests to backend

---

## Troubleshooting

### Problem: "Could not load keystore"

**Solution:**

-   Ensure `keystore.p12` exists in `backend/src/main/resources/`
-   Verify the password in `.env` matches the one used to create the keystore
-   Regenerate the keystore if needed

### Problem: "JWT_SECRET must not be empty"

**Solution:**

-   Ensure `.env` file exists in `backend/` directory
-   Verify all required variables are set
-   Check IntelliJ is loading the `.env` file (EnvFile plugin or manual config)

### Problem: "Address already in use (port 8081)"

**Solution:**

```bash
# Find what's using the port
sudo lsof -i :8081

# Kill the process
kill -9 <PID>
```

### Problem: IntelliJ not recognizing Spring Boot

**Solution:**

-   Ensure you opened the `backend` folder (not the root `QuickClock` folder)
-   File → Invalidate Caches → Invalidate and Restart
-   Reimport Gradle project: Right-click `build.gradle` → Reload Gradle Project

### Problem: Can't access https://localhost:8081

**Solution:**

-   Your browser will show a security warning for self-signed certificates
-   Click "Advanced" → "Proceed to localhost" (or similar)
-   This is expected behavior for development

---

## Quick Reference: File Locations

```
backend/
├── .env                           # Your environment variables (git ignored)
├── .env.example                   # Template to copy
├── build.gradle                   # Gradle configuration
├── src/
│   ├── main/
│   │   ├── java/                  # Java source code
│   │   └── resources/
│   │       ├── application.yml    # Main config
│   │       ├── application-dev.yml # Dev profile config
│   │       └── keystore.p12       # SSL certificate (git ignored)
│   └── test/                      # Test code
└── gradlew                        # Gradle wrapper (use this)
```

---

## Team Collaboration

### When sharing this project:

**DO commit:**

-   `.env.example` files
-   Source code
-   `build.gradle`, `settings.gradle`
-   `application*.yml` files (without secrets)

**DON'T commit:**

-   `.env` files with real secrets
-   `keystore.p12` files
-   IDE-specific configs (`.idea/`, `*.iml`)
-   Build outputs (`build/`, `*.jar`)

### For new team members:

1. Clone the repo
2. Copy `.env.example` to `.env`
3. Fill in their own secrets
4. Generate their own keystore
5. Run in IntelliJ

---

## Next Steps

-   Review [ENV-SETUP.md](../ENV-SETUP.md) for detailed environment documentation
-   Check [DOCKER-DEV.md](../DOCKER-DEV.md) for Docker setup
-   Run tests: `./gradlew test`
-   Build JAR: `./gradlew bootJar`
