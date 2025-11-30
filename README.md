# HealthLink

HealthLink is a comprehensive healthcare platform featuring a Flutter mobile application and a Spring Boot backend. It provides secure telemedicine, appointment scheduling, and health record management with strict PHI protection and RBAC compliance.

## Project Structure

- `healthlink_mobile`: Flutter mobile application (Patient & Doctor portals).
- `healthlink_backend`: Spring Boot backend services.
- `healthlink_files`: Documentation and assets.

## Prerequisites

- **Java:** JDK 21 LTS
- **Flutter:** 3.38 (Dart 3.10)
- **Docker:** Docker Desktop (for local environment)

## Getting Started

### Backend

1. Navigate to `healthlink_backend`:
   ```bash
   cd healthlink_backend
   ```

2. Start infrastructure services (PostgreSQL, Redis, RabbitMQ, MinIO, Elasticsearch, Coturn, etc.):
   ```bash
   docker-compose up -d
   ```

3. Verify services are healthy:
   ```bash
   docker ps --filter "name=healthlink" --format "table {{.Names}}\t{{.Status}}"
   ```

4. Run the application:
   ```bash
   ./gradlew bootRun
   ```

5. Access services:
   - **API:** http://localhost:8080
   - **Swagger UI:** http://localhost:8080/swagger-ui.html
   - **API Docs (JSON):** http://localhost:8080/api-docs
   - **Actuator Health:** http://localhost:8080/actuator/health
   - **MinIO Console:** http://localhost:9001 (admin/healthlink_minio_password_123)
   - **RabbitMQ Management:** http://localhost:15672 (healthlink/healthlink_rabbit_pass)
   - **Grafana:** http://localhost:3000 (admin/healthlink_grafana_pass)
   - **Jaeger Tracing:** http://localhost:16686
   - **MailHog:** http://localhost:8025

### Mobile

1. Navigate to `healthlink_mobile`:
   ```bash
   cd healthlink_mobile
   ```

2. Install dependencies:
   ```bash
   flutter pub get
   ```

3. Run code generation (if needed):
   ```bash
   flutter pub run build_runner build --delete-conflicting-outputs
   ```

4. Run the app (debug mode):
   ```bash
   flutter run
   ```

5. Run tests:
   ```bash
   flutter test
   flutter analyze
   ```

## Key Features

- **Telemedicine:** WebRTC-based video consultations with Janus Gateway and Coturn TURN server.
- **Appointments:** Real-time scheduling and management with conflict detection.
- **Medical Records:** Secure storage and retrieval of PHI with field-level encryption.
- **Prescriptions:** Digital prescriptions with drug interaction checks and allergy warnings.
- **Lab Results:** Structured lab result management with reference ranges and trending.
- **File Storage:** MinIO-based S3-compatible storage for medical images and documents.
- **Payments:** Integrated payment gateway (JazzCash) with wallet and transaction history.
- **Notifications:** Real-time notifications via RabbitMQ and email (MailHog for dev).
- **Security:**
  - **PHI Protection:** AES-256 field-level encryption, audit logging via `SafeLogger`.
  - **RBAC:** Role-Based Access Control (PATIENT, DOCTOR, STAFF, ORGANIZATION, ADMIN, PLATFORM_OWNER).
  - **Authentication:** JWT with short-lived access tokens (15m) and rotating refresh tokens.
  - **Rate Limiting:** Bucket4j + Redis-backed rate limiting per user/endpoint.
  - **Forced Logout:** Token revocation via `tokensRevokedAt` timestamp.
- **Observability:**
  - **Metrics:** Prometheus + Grafana dashboards.
  - **Tracing:** OpenTelemetry + Jaeger distributed tracing.
  - **Logging:** Structured JSON logs with context propagation.
  - **Error Tracking:** Sentry integration (production).

## Architecture

### Backend (Spring Boot 3.5.8 + Java 21 LTS)
- **Hexagonal Architecture:** Domain services, infrastructure adapters, REST controllers.
- **Database:** PostgreSQL 18.1 with Hibernate DDL auto-update.
- **Caching:** Redis for rate limiting, session management, and caching.
- **Messaging:** RabbitMQ for async notifications and event processing.
- **Storage:** MinIO (S3-compatible) for file uploads.
- **Search:** Elasticsearch for full-text search (appointments, patients, records).
- **WebRTC:** Janus Gateway (SFU) + Coturn TURN server for video calls.

### Mobile (Flutter 3.38 + Dart 3.10)
- **Clean Architecture:** Domain, Data, Presentation layers.
- **State Management:** Bloc/Cubit with Equatable.
- **Localization:** English + Urdu with RTL support.
- **Theming:** Design tokens, no hardcoded colors/text.
- **Progressive PHI Disclosure:** List screens show non-PHI summaries; detail screens require authentication.

## WebRTC Setup (Coturn TURN Server)

The backend uses **Coturn** as a TURN server for NAT traversal in WebRTC video calls. Configuration is in `docker-compose.yml` and `docker/coturn/turnserver.conf`.

### Configuration

**Docker Compose** (`docker-compose.yml`):
```yaml
coturn:
  image: coturn/coturn:latest
  container_name: healthlink-coturn
  restart: unless-stopped
  ports:
    - "3478:3478/tcp"    # TURN/STUN
    - "3478:3478/udp"
    - "5349:5349/tcp"    # TURNS (TLS)
    - "5349:5349/udp"
    - "49152-49252:49152-49252/udp"  # Relay ports (100 ports for dev)
  volumes:
    - ./docker/coturn/turnserver.conf:/etc/coturn/turnserver.conf:ro
  environment:
    - DETECT_EXTERNAL_IP=yes
    - DETECT_RELAY_IP=yes
```

**Application Config** (`application.yml`):
```yaml
healthlink:
  webrtc:
    signaling:
      url: ws://localhost:8080/signaling
    jwt:
      expiration: 3600000  # 1 hour
    ice-servers:
      - urls: stun:stun.l.google.com:19302
      - urls: turn:coturn:3478
        username: healthlink
        credential: healthlink_turn_secret_2025
      - urls: turns:coturn:5349
        username: healthlink
        credential: healthlink_turn_secret_2025
```

**Credentials** (Static Long-Term):
- **Username:** `healthlink`
- **Password:** `healthlink_turn_secret_2025`

**Ports (Windows Development):**
- Relay port range reduced to **100 ports** (49152-49252) to avoid Windows reserved port conflicts.
- For production, expand to full range (49152-65535) on Linux.

### Testing Coturn Connectivity

```bash
# Check Coturn logs
docker logs healthlink-coturn --tail 50

# Test TURN connectivity (from host, requires coturn-utils)
turnutils_uclient -v -u healthlink -w healthlink_turn_secret_2025 coturn
```

**Expected Logs:**
```
0: : INFO: IO method (main listener thread): epoll (with changelist)
0: : INFO: relay 127.0.0.1 addr 0.0.0.0:3478, TURN listener addr 0.0.0.0:3478
0: : INFO: Number of relay threads: 50
```

### Troubleshooting

**Port Binding Errors:**
- Windows reserves many UDP ports. If you see "access forbidden" errors, reduce the relay port range in `docker-compose.yml` and `turnserver.conf`.
- Example: Use 100-200 ports (49152-49252) instead of 16k+ ports.

**Health Check Unhealthy:**
- Container may report unhealthy if `turnutils_uclient` is not available inside the image.
- Verify functional operation by checking logs for "TCP/UDP listeners" messages.
- Health check can be disabled or replaced with a simpler connectivity test.

**TLS/DTLS (Production):**
- For production, configure TLS certificates in `turnserver.conf`:
  ```
  cert=/etc/coturn/certs/turn_server_cert.pem
  pkey=/etc/coturn/certs/turn_server_pkey.pem
  ```
- Use Let's Encrypt or corporate CA certificates.

## API Documentation

### Swagger UI (OpenAPI 3.0)

**Interactive API Explorer:**
- **URL:** http://localhost:8080/swagger-ui.html
- **Features:** Try-it-out requests, schema validation, authentication testing.

**OpenAPI JSON Spec:**
- **URL:** http://localhost:8080/api-docs
- **Use:** Import into Postman, Insomnia, or API testing tools.

### Key Endpoints

**Authentication:**
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - Login (returns access + refresh tokens)
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/logout` - Logout (revokes tokens)

**Appointments:**
- `GET /api/v1/appointments` - List appointments (patient/doctor specific)
- `POST /api/v1/appointments` - Create appointment
- `GET /api/v1/appointments/{id}` - Get appointment details
- `PUT /api/v1/appointments/{id}` - Update appointment
- `DELETE /api/v1/appointments/{id}` - Cancel appointment

**Video Calls:**
- `POST /api/v1/video-calls/token` - Get WebRTC token (includes ICE servers)
- `GET /api/v1/video-calls/{appointmentId}/session` - Get Janus session info
- `POST /api/v1/video-calls/{appointmentId}/end` - End video call

**Medical Records:**
- `GET /api/v1/records` - List records (patient-specific)
- `POST /api/v1/records` - Create record
- `GET /api/v1/records/{id}` - Get record details

**File Uploads (MinIO):**
- `POST /api/v1/files/upload` - Upload file (max 5MB)
- `GET /api/v1/files/{id}/download` - Download file (presigned URL)

**Authentication Header:**
```
Authorization: Bearer <access_token>
```

**Rate Limiting:**
- Default: 100 requests/minute per user.
- Headers: `X-Rate-Limit-Remaining`, `X-Rate-Limit-Retry-After-Seconds`.

## Documentation

- [Feature Specifications](healthlink_files/feature_specs.md)
- [Updated Feature Specifications](healthlink_files/updated_feature_specs.md)
- [Deployment Guide](healthlink_backend/DEPLOYMENT_GUIDE.md)
- [API Integration Guide](API_INTEGRATION_GUIDE.md)
- [Database Schema](DATABASE_SCHEMA.md)
- [Release Guide](RELEASE_GUIDE.md)

## License

Proprietary. All rights reserved.
