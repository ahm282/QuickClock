# QuickClock

Employee time tracking system with QR code clock-in/out functionality.

## 🚀 Quick Start Guide

Choose your setup method:

### Option 1: Run Locally in IntelliJ (Recommended for Development)

```bash
# 1. Clone repository
git clone <repo-url>
cd QuickClock

# 2. Setup backend
cd backend
./setup-backend.sh

# 3. Open backend folder in IntelliJ IDEA
# 4. Run using the provided run configuration
```

**See:** [SETUP.md](SETUP.md) for detailed instructions

### Option 2: Run with Docker (Full Stack)

```bash
# 1. Clone repository
git clone <repo-url>
cd QuickClock

# 2. Setup environment
cp .env.example .env
nano .env  # Edit with your values

# 3. Generate SSL certificates
./generate-ssl-certs.sh

# 4. Start containers
docker compose -f docker-compose.dev.yml up --build

# Access at: https://localhost
```

**See:** [DOCKER-DEV.md](DOCKER-DEV.md) for detailed Docker setup

## 📁 Project Structure

```
QuickClock/
├── backend/              # Spring Boot 4 API (Java 21)
│   ├── src/
│   ├── .env             # Backend environment (git ignored)
│   ├── .env.example     # Backend env template
│   └── README.md        # Backend-specific docs
│
├── QuickClock-UI/       # Angular 21 Frontend
│   ├── src/
│   └── Dockerfile.dev
│
├── nginx/               # Reverse proxy configuration
│   └── conf.d/
│
├── certs/              # SSL certificates (git ignored)
│
├── .env                # Docker Compose environment (git ignored)
├── .env.example        # Docker env template
├── docker-compose.dev.yml
│
└── Documentation:
    ├── README.md         # This file
    ├── SETUP.md          # Local development setup
    ├── DOCKER-DEV.md     # Docker setup guide
    └── ENV-SETUP.md      # Environment variables reference
```

## 🛠 Technology Stack

### Backend

-   Java 21
-   Spring Boot 4.0.0
-   Spring Security + JWT
-   Spring Data JPA
-   H2 Database (dev) / PostgreSQL (prod)
-   Gradle

### Frontend

-   Angular 21
-   TypeScript
-   TailwindCSS + DaisyUI
-   RxJS
-   ZXing (QR code scanning)

### Infrastructure

-   Docker & Docker Compose
-   Nginx (reverse proxy)
-   HTTPS/SSL (self-signed for dev)

## 📚 Documentation

-   **[SETUP.md](SETUP.md)** - Complete guide for running locally in IntelliJ
-   **[DOCKER-DEV.md](DOCKER-DEV.md)** - Docker development environment setup
-   **[ENV-SETUP.md](ENV-SETUP.md)** - Environment variables reference
-   **[backend/README.md](backend/README.md)** - Backend-specific documentation

## 🔑 Environment Setup

### Required Environment Variables

```bash
# JWT Configuration
JWT_SECRET=<generate-with-openssl-rand-base64-64>
JWT_ISSUER=QuickClock-API
JWT_AUDIENCE=QuickClock-App

# SSL Configuration
SSL_KEYSTORE_PASSWORD=<your-secure-password>

# Admin Credentials
SUPER_ADMIN_USERNAME=superadmin
SUPER_ADMIN_PASSWORD=<your-secure-password>

# Kiosk Credentials
KIOSK_USERNAME=kiosk
KIOSK_PASSWORD=<your-secure-password>
```

See [ENV-SETUP.md](ENV-SETUP.md) for complete reference.

## 🏃 Running the Application

### Backend Only (IntelliJ)

```bash
cd backend
./gradlew bootRun
# Access: https://localhost:8081
```

### Frontend Only

```bash
cd QuickClock-UI
npm install
npm start
# Access: https://localhost:5173
```

### Full Stack (Docker)

```bash
docker compose -f docker-compose.dev.yml up
# Access: https://localhost
```

## 🧪 Testing

### Backend Tests

```bash
cd backend
./gradlew test
```

### Build Backend JAR

```bash
cd backend
./gradlew bootJar
# Output: build/libs/QuickClock-0.0.1-SNAPSHOT.jar
```

## 🔒 Security

-   HTTPS enforced in development and production
-   JWT-based authentication
-   Secure HTTP-only cookies
-   Role-based access control (RBAC)
-   Password strength validation
-   Rate limiting

## 📡 API Endpoints

Once running, access:

-   **API Base**: `https://localhost:8081/api`
-   **Health Check**: `https://localhost:8081/actuator/health`
-   **H2 Console**: `https://localhost:8081/h2-console` (dev only)

## 🎯 Features

-   ✅ Employee authentication & authorization
-   ✅ QR code generation for employees
-   ✅ QR code scanning for clock-in/out
-   ✅ Admin dashboard
-   ✅ Kiosk mode for public terminals
-   ✅ Real-time attendance tracking
-   ✅ Role-based access (Admin, User, Kiosk)

## 🤝 Contributing

1. Clone the repository
2. Follow [SETUP.md](SETUP.md) for local development
3. Create a feature branch
4. Make your changes
5. Test thoroughly
6. Submit a pull request

## 🐛 Troubleshooting

### Common Issues

**"Could not load keystore"**

```bash
cd backend
./generate-local-ssl.sh
```

**"Port already in use"**

```bash
# Find and kill process using port
sudo lsof -i :8081
kill -9 <PID>
```

**"Environment variables not loaded"**

-   Ensure `.env` files exist (copy from `.env.example`)
-   For IntelliJ: Install EnvFile plugin
-   For Docker: Check `docker-compose.dev.yml` has `env_file:` section

See individual README files for more troubleshooting help.

## 📞 Support

For detailed setup instructions, see:

-   Local development: [SETUP.md](SETUP.md)
-   Docker setup: [DOCKER-DEV.md](DOCKER-DEV.md)
-   Environment config: [ENV-SETUP.md](ENV-SETUP.md)
-   Backend specifics: [backend/README.md](backend/README.md)

## 📄 License

[Your License Here]
