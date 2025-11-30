### 1. Environment Configuration

Create a `.env` file in the `healthlink_backend` directory (or configure environment variables in your deployment environment) with the following keys:

```properties
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=healthlink
DB_USERNAME=postgres
DB_PASSWORD=your_strong_password_here

# Security
JWT_SECRET=your_very_long_and_secure_jwt_secret_key_at_least_256_bits_minimum
JWT_ACCESS_EXPIRATION=900000     # 15 minutes
JWT_REFRESH_EXPIRATION=604800000 # 7 days
PHI_ENCRYPTION_KEY=base64_encoded_32_byte_key_for_aes_256_encryption

# External Services (Required for Production)
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=healthlink
RABBITMQ_PASSWORD=healthlink_rabbit_pass
ELASTICSEARCH_HOST=localhost
ELASTICSEARCH_PORT=9200
REDIS_HOST=localhost
REDIS_PORT=6379
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=healthlink_minio
MINIO_SECRET_KEY=healthlink_minio_password_123
MINIO_BUCKET=healthlink-records

# WebRTC (Janus + Coturn)
JANUS_URL=http://localhost:8088/janus
JANUS_ADMIN_SECRET=your_janus_admin_secret
WEBRTC_SIGNALING_URL=ws://localhost:8080/signaling
STUN_SERVER=stun:stun.l.google.com:19302
TURN_SERVER=turn:coturn:3478
TURN_USERNAME=healthlink
TURN_CREDENTIAL=healthlink_turn_secret_2025
TURNS_SERVER=turns:coturn:5349

# Observability (Optional)
SENTRY_DSN=your_sentry_dsn_for_error_tracking
OTLP_ENDPOINT=http://localhost:4318
PROMETHEUS_ENABLED=true
```

### 2. Build the Application

Navigate to the backend directory and build the JAR file:

```bash
cd healthlink_backend
./gradlew clean build -x test
```

### 3. Run with Docker Compose

We provide a `docker-compose.yml` to spin up all dependencies (PostgreSQL, Redis, RabbitMQ, Elasticsearch, MinIO, Janus, Coturn).

```bash
# Start all services
docker-compose up -d

# Check service health
docker ps --filter "name=healthlink" --format "table {{.Names}}\t{{.Status}}"

# View logs for specific service
docker logs healthlink-backend --tail 50
docker logs healthlink-coturn --tail 50
```

This will start:

- **PostgreSQL 18.1**: Port 5432 (Database)
- **Redis 8.4.0**: Port 6379 (Caching, rate limiting)
- **RabbitMQ 4.1**: Port 5672 (Messaging), 15672 (Management UI)
- **Elasticsearch 9.2**: Port 9200 (Search)
- **MinIO**: Port 9000 (Object storage), 9001 (Console UI)
- **Janus Gateway 1.2**: Port 8088 (WebRTC SFU)
- **Coturn**: Ports 3478 (TURN/STUN), 5349 (TURNS), 49152-49252/udp (Relay)
- **Prometheus**: Port 9090 (Metrics)
- **Grafana**: Port 3000 (Dashboards - admin/healthlink_grafana_pass)
- **Jaeger**: Port 16686 (Distributed tracing)
- **MailHog**: Port 8025 (Email testing UI)
- **HealthLink Backend**: Port 8080

**Service Dependencies:**
- Backend depends on: PostgreSQL, Redis, RabbitMQ, Elasticsearch, MinIO
- WebRTC calls depend on: Janus, Coturn
- Observability: Prometheus, Grafana, Jaeger (optional)

**Health Checks:**
All services include health checks that run every 10-30 seconds. Services marked as `(healthy)` in `docker ps` are ready for use.

### 4. Database Migration

The application uses Hibernate `update` strategy for development. For production, ensure you have a proper migration strategy (e.g., Flyway) or let Hibernate create the schema on the first run if appropriate for your setup.

### 5. Verify Deployment

Access the API documentation (Swagger UI) at:
`http://localhost:8080/swagger-ui.html` (if enabled)
Or check the health endpoint:
`http://localhost:8080/actuator/health`

## Mobile App Deployment

### 1. Configuration

Update `lib/core/constants/api_constants.dart` (or equivalent config) to point to your backend URL.

```dart
const String BASE_URL = "http://<your-backend-ip>:8080/api/v1";
```

### 2. Build for Android

```bash
cd healthlink_mobile
flutter build apk --release
```

The APK will be in `build/app/outputs/flutter-apk/app-release.apk`.

### 3. Build for iOS (Mac only)

```bash
cd healthlink_mobile
flutter build ios --release
```

Open `ios/Runner.xcworkspace` in Xcode to archive and publish.

## WebRTC Configuration (Coturn TURN Server)

### Coturn Setup

The Coturn TURN server is essential for WebRTC calls when clients are behind NAT/firewalls. Configuration is in `docker/coturn/turnserver.conf`.

**Key Settings:**
- **Listening Ports**: 3478 (TURN/STUN), 5349 (TURNS with TLS)
- **Relay Ports**: 49152-49252 (100 UDP ports for development)
- **Authentication**: Long-term credentials (username: healthlink, password: healthlink_turn_secret_2025)
- **Relay Threads**: 50 (adjustable based on expected concurrent calls)

**Production Recommendations:**
1. **TLS Certificates**: Configure TLS for TURNS (port 5349):
   ```
   cert=/etc/coturn/certs/turn_server_cert.pem
   pkey=/etc/coturn/certs/turn_server_pkey.pem
   ```
   Use Let's Encrypt or corporate CA certificates.

2. **Expand Relay Ports**: Increase to full range (49152-65535) on Linux:
   ```yaml
   ports:
     - "49152-65535:49152-65535/udp"
   ```
   Windows has reserved port conflicts—use 500-1000 ports for dev.

3. **External IP Detection**: Coturn auto-detects external IP (`DETECT_EXTERNAL_IP=yes`). Verify in logs:
   ```bash
   docker logs healthlink-coturn | grep "External IP"
   ```

4. **Time-Limited Credentials**: Replace static credentials with HMAC-based tokens:
   - Use `static-auth-secret` instead of user/password
   - Generate tokens with TTL in VideoCallService
   - Reference: https://tools.ietf.org/html/rfc5766#section-15.4

**Testing Connectivity:**
```bash
# Check Coturn logs
docker logs healthlink-coturn --tail 50

# Expected output:
# 0: : INFO: relay 127.0.0.1 addr 0.0.0.0:3478, TURN listener addr 0.0.0.0:3478
# 0: : INFO: Number of relay threads: 50

# Test from host (requires coturn-utils)
turnutils_uclient -v -u healthlink -w healthlink_turn_secret_2025 coturn
```

**Common Issues:**
- **Port Binding Errors**: Windows reserves many UDP ports. Reduce relay range to 100-200 ports for dev.
- **Health Check Unhealthy**: Container may report unhealthy if `turnutils_uclient` not available. Verify logs show "TCP/UDP listeners" active.
- **NAT Traversal Failures**: Ensure firewall allows UDP on relay port range (49152-49252).

## Troubleshooting

- **Database Connection Refused**: Ensure Docker containers are running and ports are not blocked. Check `docker-compose logs postgres`.
- **JWT Errors**: Verify `JWT_SECRET` is identical in all instances and meets length requirements (32+ bytes).
- **File Upload Failures**: Check MinIO configuration and bucket permissions. Ensure `MINIO_ENDPOINT` is reachable from the backend container.
  ```bash
  # Check MinIO health
  curl http://localhost:9000/minio/health/live
  
  # Access MinIO console
  # URL: http://localhost:9001
  # Username: healthlink_minio
  # Password: healthlink_minio_password_123
  ```
- **RabbitMQ Connection Issues**: Verify RabbitMQ management UI accessible at http://localhost:15672 (healthlink/healthlink_rabbit_pass).
- **WebRTC Call Failures**: 
  - Check Janus Gateway logs: `docker logs healthlink-janus --tail 50`
  - Verify Coturn is running and listening on ports 3478, 5349
  - Test STUN server: `stun:stun.l.google.com:19302` (should always work)
  - Check browser console for ICE candidate gathering errors
- **Rate Limiting Errors (429)**: Redis required for Bucket4j rate limiting. Verify Redis container healthy.
- **Elasticsearch Not Available**: If search features fail, check `docker logs healthlink-elasticsearch`. Elasticsearch requires 4GB RAM.

## Security Notes

### Production Checklist

- **Change Default Passwords**: Update all default passwords in `docker-compose.yml` and `.env` before production use.
- **HTTPS**: Configure SSL/TLS termination (e.g., Nginx, Traefik) in front of the backend API.
- **Firewall**: Restrict access to database and internal service ports (5432, 6379, etc.) to only the backend application.
- **PHI Encryption**: Ensure `PHI_ENCRYPTION_KEY` is a 32-byte base64-encoded key stored securely (AWS Secrets Manager, HashiCorp Vault).
- **JWT Secret**: Use a cryptographically secure random 256-bit key. Rotate periodically.
- **TURN Credentials**: Replace static credentials with time-limited HMAC tokens (see Coturn section).
- **Rate Limiting**: Enable rate limiting in production (`healthlink.rate-limit.enabled=true`). Default: 100 requests/minute/user.
- **CORS**: Update `allowed-origins` in SecurityConfig to whitelist only your mobile app domains.
- **Database Encryption**: Enable PostgreSQL SSL/TLS (`ssl=true` in JDBC URL) and encrypt database backups.
- **Network Isolation**: Use Docker networks to isolate backend, database, and external services. Only expose necessary ports.
- **Secrets Management**: Do not commit `.env` files or credentials to version control. Use environment-specific secrets injection.

### PHI Compliance (HIPAA-like)

- **Audit Logging**: All PHI access logged via `SafeLogger.audit()` with actor, action, resource, reason.
- **Field-Level Encryption**: Sensitive fields (SSN, CNIC, phone) encrypted at rest with AES-256.
- **Access Control**: RBAC enforced at controller/service layer. Patients cannot access other patients' PHI.
- **Token Revocation**: Forced logout via `tokensRevokedAt` timestamp. Update timestamp to revoke all user tokens immediately.
- **Data Retention**: Implement soft deletes (`deleted_at` timestamp) for GDPR/HIPAA compliance. Hard delete after retention period.
- **Monitoring**: Sentry integration for error tracking (production). Exclude PHI from exception messages.
