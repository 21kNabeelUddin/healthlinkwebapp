# HealthLink+

HealthLink+ is a comprehensive healthcare platform web application providing secure telemedicine, appointment scheduling, and health record management with strict PHI protection and RBAC compliance. Built with Next.js frontend and Spring Boot backend.

## Project Structure

- `frontend`: Next.js 14 web application (Patient, Doctor, and Admin portals)
- `healthlink_backend`: Spring Boot 3 backend services
- `healthlink_files`: Documentation and assets

## Prerequisites

- **Java:** JDK 21 LTS
- **Node.js:** 18+ and npm/yarn
- **Docker:** Docker Desktop (for local infrastructure services)

## Getting Started

### Backend Setup

1. Navigate to `healthlink_backend`:
   ```bash
   cd healthlink_backend
   ```

2. Configure environment variables:
   - Copy `.env.example` to `.env` (if available) or create `.env` with required variables
   - See `SETUP_ENV.md` for detailed configuration

3. Start infrastructure services (PostgreSQL, Redis, RabbitMQ, MinIO, etc.):
   ```bash
   docker-compose up -d
   ```

4. Verify services are healthy:
   ```bash
   docker ps --filter "name=healthlink" --format "table {{.Names}}\t{{.Status}}"
   ```

5. Run the application:
   ```bash
   ./gradlew bootRun
   ```

6. Access backend services:
   - **API:** http://localhost:8080
   - **Swagger UI:** http://localhost:8080/swagger-ui.html
   - **API Docs (JSON):** http://localhost:8080/api-docs
   - **Actuator Health:** http://localhost:8080/actuator/health
   - **MinIO Console:** http://localhost:9001 (admin/healthlink_minio_password_123)
   - **RabbitMQ Management:** http://localhost:15672 (healthlink/healthlink_rabbit_pass)
   - **Grafana:** http://localhost:3000 (admin/healthlink_grafana_pass)
   - **Jaeger Tracing:** http://localhost:16686
   - **MailHog:** http://localhost:8025

### Frontend Setup

1. Navigate to `frontend`:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Create `.env.local` (optional, defaults are set):
   ```env
   NEXT_PUBLIC_API_URL=http://localhost:8080
   ```

4. Run the development server:
   ```bash
   npm run dev
   ```

5. Open [http://localhost:3000](http://localhost:3000) in your browser

## Key Features

### Patient Portal
- **Authentication:** Sign up, login, OTP verification, forgot password
- **Doctor Discovery:** Browse and search doctors by specialization, location, and filters
- **Appointment Booking:** Book online (Zoom) or on-site appointments with clinic selection
- **Medical History:** Complete CRUD operations for medical records
- **Appointment Management:** View, manage, and track appointment status
- **Notifications:** Real-time appointment updates and reminders
- **Payments:** View payment history and transaction details

### Doctor Portal
- **Authentication:** Sign up, login, OTP verification, forgot password
- **Clinic Management:** Create, update, activate/deactivate multiple clinic locations
- **Appointment Management:** View appointments grouped by clinic, confirm, reject, or complete
- **Emergency Patients:** Create patient accounts on-the-spot for walk-in appointments
- **Prescriptions:** Create and manage digital prescriptions
- **Analytics:** View appointment statistics and clinic performance
- **Profile Management:** Update profile, specialization, and consultation fees

### Admin Portal
- **Dashboard:** System-wide analytics and metrics
- **User Management:** Approve/reject doctor registrations
- **PMDC Verification:** Verify doctor PMDC licenses
- **Clinic Oversight:** Monitor and manage all clinics
- **Appointment Monitoring:** View all appointments across the platform
- **Patient Management:** View and manage patient accounts

### Core Platform Features
- **Telemedicine:** Zoom video call integration for online consultations
- **Appointments:** Real-time scheduling with conflict detection and availability checks
- **Medical Records:** Secure storage and retrieval of PHI with field-level encryption
- **Prescriptions:** Digital prescriptions with structured medication management
- **File Storage:** MinIO-based S3-compatible storage for medical images and documents
- **Notifications:** Real-time notifications via RabbitMQ and email
- **Security:**
  - **PHI Protection:** AES-256 field-level encryption, audit logging via `SafeLogger`
  - **RBAC:** Role-Based Access Control (PATIENT, DOCTOR, STAFF, ORGANIZATION, ADMIN, PLATFORM_OWNER)
  - **Authentication:** JWT with 4-hour access tokens and rotating refresh tokens
  - **Rate Limiting:** Bucket4j + Redis-backed rate limiting per user/endpoint
  - **Forced Logout:** Token revocation via `tokensRevokedAt` timestamp
- **Observability:**
  - **Metrics:** Prometheus + Grafana dashboards
  - **Tracing:** OpenTelemetry + Jaeger distributed tracing
  - **Logging:** Structured JSON logs with context propagation
  - **Error Tracking:** Sentry integration (production)

## Architecture

### Backend (Spring Boot 3.5.8 + Java 21 LTS)
- **Hexagonal Architecture:** Domain services, infrastructure adapters, REST controllers
- **Database:** PostgreSQL 18.1 with Hibernate DDL auto-update
- **Caching:** Redis for rate limiting, session management, and caching
- **Messaging:** RabbitMQ for async notifications and event processing
- **Storage:** MinIO (S3-compatible) for file uploads
- **Search:** Database-backed doctor search (Elasticsearch optional)
- **WebRTC:** Janus Gateway (SFU) + Coturn TURN server for video calls (future)

### Frontend (Next.js 14 + TypeScript)
- **Framework:** Next.js 14 with App Router
- **Language:** TypeScript for type safety
- **Styling:** Tailwind CSS with Radix UI components
- **State Management:** React Context API for authentication
- **Forms:** React Hook Form for form management and validation
- **HTTP Client:** Axios for API communication
- **Notifications:** Sonner (toast notifications)
- **Icons:** Lucide React
- **Date Handling:** date-fns for date formatting

## WebRTC Setup (Coturn TURN Server)

The backend includes configuration for **Coturn** as a TURN server for NAT traversal in WebRTC video calls. Configuration is in `docker-compose.yml` and `docker/coturn/turnserver.conf`.

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
- `POST /api/v1/auth/forgot-password` - Initiate password reset
- `POST /api/v1/auth/reset-password` - Reset password with OTP

**Appointments:**
- `GET /api/v1/appointments` - List appointments (patient/doctor specific)
- `POST /api/v1/appointments` - Create appointment
- `GET /api/v1/appointments/{id}` - Get appointment details
- `PUT /api/v1/appointments/{id}` - Update appointment
- `DELETE /api/v1/appointments/{id}` - Cancel appointment

**Doctors:**
- `GET /api/v1/search/doctors` - Search and list doctors (with filters)
- `GET /api/v1/facilities/doctor/{doctorId}` - Get doctor's clinics

**Clinics:**
- `GET /api/v1/facilities` - List clinics
- `POST /api/v1/facilities` - Create clinic (doctor only)
- `PUT /api/v1/facilities/{id}` - Update clinic
- `POST /api/v1/facilities/{id}/activate` - Activate clinic

**Emergency Patients:**
- `POST /api/v1/doctors/{doctorId}/emergency/patient` - Create emergency patient
- `POST /api/v1/doctors/{doctorId}/emergency/patient-and-appointment` - Create patient and appointment

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

## Development

### Backend Development

```bash
cd healthlink_backend
./gradlew bootRun
```

The backend will automatically reload on code changes (if using Spring Boot DevTools).

### Frontend Development

```bash
cd frontend
npm run dev
```

The frontend will hot-reload on code changes.

### Building for Production

**Backend:**
```bash
cd healthlink_backend
./gradlew build
java -jar build/libs/healthlink-*.jar
```

**Frontend:**
```bash
cd frontend
npm run build
npm run start
```

## Environment Variables

### Backend (`.env` in `healthlink_backend/`)

See `healthlink_backend/SETUP_ENV.md` for complete list. Key variables:
- `DATABASE_URL` - PostgreSQL connection string
- `SPRING_DATASOURCE_URL` - JDBC URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `JWT_SECRET` - JWT signing secret (min 32 characters)
- `PHI_ENCRYPTION_KEY` - Base64-encoded 256-bit key for PHI encryption
- `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD` - Email service configuration

### Frontend (`.env.local` in `frontend/`)

- `NEXT_PUBLIC_API_URL` - Backend API URL (default: `http://localhost:8080`)

## Documentation

- [Feature Specifications](healthlink_files/feature_specs.md)
- [Updated Feature Specifications](healthlink_files/updated_feature_specs.md)
- [Deployment Guide](healthlink_backend/DEPLOYMENT_GUIDE.md)
- [API Integration Guide](API_INTEGRATION_GUIDE.md)
- [Database Schema](DATABASE_SCHEMA.md)
- [Release Guide](RELEASE_GUIDE.md)
- [Recent Changes](RECENT_CHANGES.md)
- [TODO List](TODO.md)

## License

Proprietary. All rights reserved.
