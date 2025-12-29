# QuickClock Production Deployment Guide

## Prerequisites

-   **Azure VM**: Ubuntu 24.04 LTS
-   **Domain**: DNS A record pointing to your VM's public IP
-   **Docker**: Docker Engine 24+ and Docker Compose v2+
-   **Ports**: 80 and 443 open in Azure Network Security Group

## Quick Start

### 1. Install Docker on Ubuntu 24.04

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com | sudo sh

# Add your user to docker group
sudo usermod -aG docker $USER

# Log out and back in, then verify
docker --version
docker compose version
```

### 2. Clone and Configure

```bash
# Clone repository (or upload your files)
git clone https://github.com/your-repo/QuickClock.git
cd QuickClock

# Copy and edit environment file
cp .env.template .env
nano .env
```

### 3. Configure Environment Variables

Edit `.env` with your production values:

```bash
# Required changes:
DOMAIN_NAME=your-domain.com
CERTBOT_EMAIL=your-email@example.com
POSTGRES_PASSWORD=$(openssl rand -base64 32)
JWT_SECRET=$(openssl rand -base64 64)
SUPER_ADMIN_PASSWORD=YourSecurePassword123!
KIOSK_PASSWORD=YourKioskPassword123!

# First run only:
APP_BOOTSTRAP_ENABLED=true
```

### 4. Initialize SSL Certificates

```bash
chmod +x init-letsencrypt.sh
./init-letsencrypt.sh
```

### 5. Start Services

```bash
docker compose -f docker-compose.prod.yml up -d
```

### 6. Verify Deployment

```bash
# Check all services are running
docker compose -f docker-compose.prod.yml ps

# View logs
docker compose -f docker-compose.prod.yml logs -f

# Check certificate
docker compose -f docker-compose.prod.yml exec nginx nginx -t
```

### 7. Disable Bootstrap (Important!)

After first successful startup:

```bash
# Edit .env
nano .env
# Change: APP_BOOTSTRAP_ENABLED=false

# Restart services
docker compose -f docker-compose.prod.yml up -d
```

## Architecture

```
                    Internet
                        │
                        ▼
                   ┌─────────┐
                   │  Nginx  │ (Port 80/443)
                   │   SSL   │
                   └────┬────┘
                        │
         ┌──────────────┼──────────────┐
         │              │              │
         ▼              ▼              ▼
    ┌─────────┐   ┌──────────┐   ┌──────────┐
    │Frontend │   │ Backend  │   │ Certbot  │
    │ Angular │   │ Spring   │   │          │
    │ :80     │   │ :8081    │   │ (renewal)│
    └─────────┘   └────┬─────┘   └──────────┘
                       │
                       ▼
                  ┌──────────┐
                  │PostgreSQL│
                  │  :5432   │
                  └──────────┘
```

## Useful Commands

### Service Management

```bash
# Start all services
docker compose -f docker-compose.prod.yml up -d

# Stop all services
docker compose -f docker-compose.prod.yml down

# Restart a specific service
docker compose -f docker-compose.prod.yml restart backend

# View logs
docker compose -f docker-compose.prod.yml logs -f [service_name]

# Check service status
docker compose -f docker-compose.prod.yml ps
```

### Database Operations

```bash
# Connect to database
docker compose -f docker-compose.prod.yml exec database psql -U postgres -d quickclock

# Backup database
docker compose -f docker-compose.prod.yml exec database pg_dump -U postgres quickclock > backup_$(date +%Y%m%d).sql

# Restore database
cat backup.sql | docker compose -f docker-compose.prod.yml exec -T database psql -U postgres -d quickclock
```

### Adding Employees to Production

#### Option 1: Single Employee (Interactive)

```bash
# Generate credentials for one employee
./scripts/generate-user-credentials.sh

# This will prompt for:
# - Username
# - Display Name (English)
# - Display Name (Arabic)
# - Password
# - Role selection

# The script generates SQL that you can execute in psql
```

#### Option 2: Bulk Import from CSV

```bash
# 1. Create CSV file (see scripts/employees.csv.example)
cp scripts/employees.csv.example employees.csv
nano employees.csv  # Edit with your employees

# 2. Generate SQL from CSV
./scripts/bulk-import-employees.sh employees.csv

# 3. Execute the generated SQL
docker compose -f docker-compose.prod.yml exec -T database \
  psql -U postgres -d quickclock < scripts/bulk_import_*.sql

# 4. Verify employees were created
docker compose -f docker-compose.prod.yml exec database \
  psql -U postgres -d quickclock -c \
  "SELECT username, display_name, account_type FROM users WHERE account_type='EMPLOYEE';"
```

#### Option 3: Direct SQL Execution

```bash
# Connect to database
docker compose -f docker-compose.prod.yml exec database psql -U postgres -d quickclock

# Use the insert_employee function
SELECT insert_employee(
    'john.doe',
    'John Doe',
    'جون دو',
    '$2a$10$...bcrypt_hash...',  -- Generate with generate-user-credentials.sh
    'base64_secret...',           -- Generate with generate-user-credentials.sh
    ARRAY['EMPLOYEE']
);
```

### Certificate Management

```bash
# Check certificate expiry
docker compose -f docker-compose.prod.yml exec nginx sh -c "openssl x509 -in /etc/letsencrypt/live/\$DOMAIN_NAME/cert.pem -noout -dates"

# Force certificate renewal
docker compose -f docker-compose.prod.yml exec certbot certbot renew --force-renewal

# Reload nginx after renewal
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload
```

### Troubleshooting

```bash
# Check nginx configuration
docker compose -f docker-compose.prod.yml exec nginx nginx -t

# Check backend health
curl -k https://localhost/api/health

# View backend logs
docker compose -f docker-compose.prod.yml logs backend --tail=100

# Enter container shell
docker compose -f docker-compose.prod.yml exec backend sh
```

## Security Checklist

-   [ ] Strong passwords for all accounts
-   [ ] JWT_SECRET is at least 64 characters
-   [ ] APP_BOOTSTRAP_ENABLED=false after first run
-   [ ] SEED_ENABLED=false in production
-   [ ] Database not exposed externally (no port binding)
-   [ ] Azure NSG allows only ports 80 and 443
-   [ ] Regular certificate renewal is working
-   [ ] Backup strategy in place

## Updating the Application

```bash
# Pull latest changes
git pull origin main

# Rebuild and restart
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d

# Or for zero-downtime (if you have multiple instances)
docker compose -f docker-compose.prod.yml up -d --no-deps --build backend
```

## Monitoring

Consider adding these for production monitoring:

-   Prometheus + Grafana for metrics
-   Loki for log aggregation
-   Uptime monitoring (e.g., UptimeRobot, Pingdom)

## Support

For issues, check:

1. Service logs: `docker compose -f docker-compose.prod.yml logs [service]`
2. Nginx access/error logs
3. Backend application logs at `/var/log/quickclock/`
