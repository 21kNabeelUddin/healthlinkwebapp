# HealthLink Production Release Guide

**Version:** 1.0  
**Last Updated:** 2025-11-23  
**Supported Environments:** Development, Staging, Production

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Variables](#environment-variables)
3. [Database Setup](#database-setup)
4. [Backend Deployment](#backend-deployment)
5. [Mobile App Deployment](#mobile-app-deployment)
6. [Docker Deployment](#docker-deployment)
7. [Monitoring & Logging](#monitoring--logging)
8. [Security Checklist](#security-checklist)
9. [Rollback Procedures](#rollback-procedures)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Tools

- **Java 21 LTS** (OpenJDK or Oracle JDK)
- **Gradle 9.2.1+**
- **Docker** & **Docker Compose** (latest stable)
- **PostgreSQL 18.1+**
- **Redis 8.4+**
- **Node.js 18+** (for any tooling)
- **Flutter SDK 3.38** (Dart 3.10)
- **Git**

### System Requirements

- **Backend Server**: 4 CPU cores, 8GB RAM minimum (16GB recommended)
- **Database Server**: 4 CPU cores, 8GB RAM, 100GB storage minimum
- **Redis Server**: 2 CPU cores, 4GB RAM

---

## Environment Variables

### Backend (`healthlink_backend`)

Create `.env` file or set environment variables:

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/healthlink_db
SPRING_DATASOURCE_USERNAME=healthlink_user
SPRING_DATASOURCE_PASSWORD=<strong_database_password>

# Redis Configuration
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=<redis_password>  # Optional

# JWT Configuration
HEALTHLINK_JWT_SECRET=<256-bit-secret-key-base64-encoded>
HEALTHLINK_JWT_ACCESS_TOKEN_EXPIRATION=900000  # 15 minutes in ms
HEALTHLINK_JWT_REFRESH_TOKEN_EXPIRATION=604800000  # 7 days in ms

# PHI Encryption
HEALTHLINK_PHI_ENCRYPTION_KEY=<32-byte-hex-key>  # 256-bit AES key

# Elasticsearch
SPRING_ELASTICSEARCH_URIS=http://localhost:9200
SPRING_ELASTICSEARCH_USERNAME=elastic
SPRING_ELASTICSEARCH_PASSWORD=<elasticsearch_password>

# Email (If implementing external service)
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=<email_username>
SPRING_MAIL_PASSWORD=<email_app_password>

# Janus WebRTC
JANUS_WEBSOCKET_URL=ws://localhost:8188

# Application Settings
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_HEALTHLINK=DEBUG

# CORS (configure for your frontend domains)
HEALTHLINK_CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://mobile.yourdomain.com
```

### Mobile (`healthlink_mobile`)

Create `.env` file or configure build-time variables:

```bash
# API Configuration
API_BASE_URL=https://api.yourdomain.com
API_TIMEOUT=30000

# Feature Flags
ENABLE_ANALYTICS=true
ENABLE_CRASH_REPORTING=true
```

---

## Database Setup

### 1. Create Database

```sql
CREATE DATABASE healthlink_db;
CREATE USER healthlink_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE healthlink_db TO healthlink_user;
```

### 2. Run Initial Migration

The Spring Boot application will auto-create tables on first run (Hibernate `ddl-auto=update`).

**For Production**, use Flyway or Liquibase for controlled migrations:

```bash
# Navigate to backend directory
cd healthlink_backend

# Run migrations (if using Flyway)
./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://localhost:5432/healthlink_db \
  -Dflyway.user=healthlink_user \
  -Dflyway.password=your_password
```

### 3. Initialize Data

Execute `docker/init-db.sql` for test/seed data (development only):

```bash
psql -U healthlink_user -d healthlink_db -f docker/init-db.sql
```

---

## Backend Deployment

### Development

```bash
cd healthlink_backend
./gradlew bootRun
```

Access: `http://localhost:8080`

### Production (JAR)

1. **Build**:

```bash
./gradlew clean build -x test
```

2. **Deploy JAR**:

```bash
java -jar build/libs/healthlink_backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:postgresql://prod-db:5432/healthlink_db \
  --spring.datasource.username=healthlink_user \
  --spring.datasource.password=$DB_PASSWORD \
  --healthlink.jwt.secret=$JWT_SECRET \
  --healthlink.phi.encryption-key=$PHI_KEY
```

3. **Systemd Service** (Linux):

Create `/etc/systemd/system/healthlink.service`:

```ini
[Unit]
Description=HealthLink Backend API
After=network.target postgresql.service redis.service

[Service]
Type=simple
User=healthlink
WorkingDirectory=/opt/healthlink
ExecStart=/usr/bin/java -jar /opt/healthlink/healthlink_backend.jar
EnvironmentFile=/opt/healthlink/.env
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl enable healthlink
sudo systemctl start healthlink
sudo systemctl status healthlink
```

---

## Mobile App Deployment

### Android

1. **Build APK**:

```bash
cd healthlink_mobile
flutter build apk --release \
  --dart-define=ENV=production \
  --dart-define=API_BASE_URL=https://api.yourdomain.com \
  --dart-define=WS_URL=wss://api.yourdomain.com/ws \
  --dart-define=SENTRY_DSN=https://your-sentry-dsn \
  --dart-define=TURN_URL=turn:turn.yourdomain.com:3478 \
  --dart-define=TURN_USER=healthlink \
  --dart-define=TURN_PASS=turn_password
```

### Dart Defines Reference

| Variable       | Description                            | Default   |
| -------------- | -------------------------------------- | --------- |
| `ENV`          | Environment (dev, staging, production) | `dev`     |
| `API_BASE_URL` | Backend API URL                        | Localhost |
| `WS_URL`       | WebSocket URL for notifications        | Localhost |
| `SENTRY_DSN`   | Sentry Error Tracking DSN              | Empty     |
| `TURN_URL`     | WebRTC TURN Server URL                 | Empty     |
| `TURN_USER`    | TURN Server Username                   | Empty     |
| `TURN_PASS`    | TURN Server Password                   | Empty     |
| `USE_MOCK_API` | Use mock data instead of real API      | `false`   |

### Push Notifications (Firebase)

1. **Android**:

   - Place `google-services.json` in `android/app/`.
   - Ensure SHA-1/SHA-256 fingerprints are added to Firebase Console.

2. **iOS**:

   - Place `GoogleService-Info.plist` in `ios/Runner/`.
   - Enable "Push Notifications" and "Background Modes" in Xcode.
   - Upload APNs Key to Firebase Console.

3. **Build**:

Output: `build/app/outputs/flutter-apk/app-release.apk`

2. **Build AAB (Google Play)**:

```bash
flutter build appbundle --release --dart-define=API_BASE_URL=https://api.yourdomain.com
```

3. **Sign APK** (if not auto-signed):

```bash
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore my-release-key.jks app-release-unsigned.apk alias_name
```

### iOS

1. **Build IPA**:

```bash
flutter build ios --release --dart-define=API_BASE_URL=https://api.yourdomain.com
```

2. **Archive in Xcode**:

- Open `ios/Runner.xcworkspace`
- Product → Archive
- Distribute to App Store / Ad-Hoc

### Web (Future)

```bash
flutter build web --release
```

Deploy `build/web` to static hosting (Nginx, Firebase Hosting, etc.)

---

## Docker Deployment

### Using Docker Compose

1. **Configure `docker-compose.yml`** environment variables

2. **Build & Start**:

```bash
docker-compose up --build -d
```

3. **Verify Services**:

```bash
docker-compose ps
docker-compose logs -f healthlink-backend
```

4. **Health Check**:

```bash
curl http://localhost:8080/actuator/health
```

### Production Deployment with Docker

1. **Build Backend Image**:

```bash
cd healthlink_backend
docker build -t healthlink/backend:1.0.0 .
```

2. **Push to Registry**:

```bash
docker tag healthlink/backend:1.0.0 registry.yourdomain.com/healthlink/backend:1.0.0
docker push registry.yourdomain.com/healthlink/backend:1.0.0
```

3. **Deploy with Orchestration** (Kubernetes, Docker Swarm, etc.)

### Nginx Reverse Proxy

Example `/etc/nginx/sites-available/healthlink`:

```nginx
server {
    listen 80;
    server_name api.yourdomain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/api.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.yourdomain.com/privkey.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded_for $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /ws {
        proxy_pass http://localhost:8188;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

---

## Monitoring & Logging

### Application Logs

**Location**: `logs/healthlink.log` (configurable in `logback-spring.xml`)

**Log Levels**: ERROR, WARN, INFO, DEBUG

**PHI Protection**: All logs sanitized via `PhiLoggingSanitizer`

### Spring Boot Actuator

Enable endpoints in `application-prod.properties`:

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
```

Access: `http://localhost:8080/actuator/health`

### Prometheus & Grafana

1. **Prometheus Config** (`prometheus.yml`):

```yaml
scrape_configs:
  - job_name: "healthlink"
    static_configs:
      - targets: ["localhost:8080"]
    metrics_path: "/actuator/prometheus"
```

2. **Grafana Dashboards**: Import Spring Boot dashboard (ID: 4701)

### Database Monitoring

- Use `pg_stat_statements` for query performance
- Monitor connection pool metrics via Actuator
- Set up alerts for slow queries (>1s)

---

## Security Checklist

✅ **Pre-Deployment**:

- [ ] All secrets externalized to environment variables
- [ ] JWT secret is 256-bit cryptographically secure
- [ ] PHI encryption key is 256-bit AES
- [ ] Database passwords are strong (16+ characters)
- [ ] Redis password configured (if exposed)
- [ ] HTTPS/TLS certificates valid and configured
- [ ] CORS origins restricted to production domains
- [ ] Rate limiting configured
- [ ] SQL injection protection verified (JPA/Hibernate)
- [ ] XSS protection headers enabled
- [ ] CSRF protection enabled for state-changing operations

✅ **Post-Deployment**:

- [ ] Verify PHI access audit logs
- [ ] Test RBAC enforcement for all roles
- [ ] Confirm email/notification security
- [ ] Scan for vulnerabilities (OWASP ZAP, Burp Suite)
- [ ] Penetration testing completed
- [ ] Security headers validated (SecurityHeaders.com)

---

## Rollback Procedures

### Backend Rollback

1. **Identify Last Good Version**:

```bash
git log --oneline
```

2. **Revert to Previous JAR**:

```bash
sudo systemctl stop healthlink
cp /opt/healthlink/backups/healthlink_backend-previous.jar /opt/healthlink/healthlink_backend.jar
sudo systemctl start healthlink
```

3. **Database Rollback** (if schema changed):

```bash
# Restore from backup
pg_restore -U healthlink_user -d healthlink_db /backups/healthlink_db_YYYYMMDD.dump
```

### Mobile Rollback

- **Android**: Upload previous APK/AAB to Google Play Console
- **iOS**: Revert to previous build in App Store Connect

---

## Troubleshooting

### Backend Won't Start

**Symptom**: Application fails to start  
**Check**:

1. Database connection: `psql -U healthlink_user -d healthlink_db`
2. Redis connection: `redis-cli ping`
3. Logs: `tail -f logs/healthlink.log`
4. Port availability: `netstat -tulpn | grep 8080`

### Database Connection Issues

**Symptom**: `org.postgresql.util.PSQLException: Connection refused`  
**Fix**:

1. Verify PostgreSQL is running: `sudo systemctl status postgresql`
2. Check `pg_hba.conf` for authentication rules
3. Verify firewall allows port 5432

### Redis Connection Failures

**Symptom**: `RedisConnectionException`  
**Fix**:

1. Verify Redis is running: `sudo systemctl status redis`
2. Test connection: `redis-cli ping`
3. Check bind address in `redis.conf`

### JWT Token Errors

**Symptom**: `401 Unauthorized` or `Invalid JWT signature`  
**Fix**:

1. Verify `HEALTHLINK_JWT_SECRET` is correctly set
2. Check token expiration settings
3. Review logs for detailed error messages

### Mobile App Can't Connect

**Symptom**: Network errors in mobile app  
**Fix**:

1. Verify `API_BASE_URL`is correct
2. Check CORS configuration in backend
3. Test API endpoint: `curl https://api.yourdomain.com/actuator/health`
4. Review network logs in Flutter DevTools

---

## Backup & Recovery

### Database Backups

**Daily Automated Backup** (cron job):

```bash
0 2 * * * pg_dump -U healthlink_user healthlink_db > /backups/healthlink_db_$(date +\%Y\%m\%d).dump
```

**Restore**:

```bash
pg_restore -U healthlink_user -d healthlink_db /backups/healthlink_db_YYYYMMDD.dump
```

### Application Backups

- JAR files: Keep last 5 versions
- Configuration files: Version controlled in Git
- Logs: Rotate daily, retain 30 days

---

## Performance Optimization

### Database

- Create indexes on frequently queried columns (`user_id`, `doctor_id`, `patient_id`)
- Enable query caching for read-heavy operations
- Monitor and optimize slow queries

### Redis

- Set appropriate eviction policies
- Monitor memory usage
- Use Redis clustering for high availability

### Application

- Enable HTTP/2
- Configure connection pooling (HikariCP defaults are good)
- Enable GZIP compression for API responses
- Use caching for expensive operations

---

## Post-Deployment Verification

1. **Health Check**: `curl https://api.yourdomain.com/actuator/health`
2. **Authentication**: Test login flow with test user
3. **RBAC**: Verify patient/doctor/staff/admin role access
4. **Video Call**: Test WebRTC connection via Janus
5. **Payments**: Verify payment flow (use test mode)
6. **Medical Records**: Test PHI encryption and access logs
7. **Search**: Verify Elasticsearch index and search results

---

## Support & Contact

- **Technical Issues**: Submit to issue tracker or engineering team
- **Security Incidents**: Contact security team immediately
- **Documentation**: Refer to [`README.md`](file:///c:/Users/aadar/Music/healthlink/README.md) and [`feature_specs.md`](file:///file:///c:/Users/aadar/Music/healthlink/healthlink_files/feature_specs.md)

---

**🎉 You're ready to deploy HealthLink to production!**
