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

### 3. Configure External Services

The application uses external services for production. Configure the following in your `.env` file:

**Required Services:**
- **PostgreSQL**: Use Neon (recommended) or any PostgreSQL 18.1+ database
  - Set `SPRING_DATASOURCE_URL` in `.env` (e.g., `jdbc:postgresql://ep-xxx.neon.tech:5432/neondb?sslmode=require`)
  - Set `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`

**Optional Services (can be disabled for basic functionality):**
- **Redis**: For caching and rate limiting (set `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`)
- **RabbitMQ**: For message queuing (set `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`)
- **Elasticsearch**: For search features (set `ELASTICSEARCH_URIS`, `ELASTICSEARCH_ENABLED=true`)
- **MinIO/S3**: For file storage (set `MINIO_ENDPOINT`, `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`)
- **Mail Service**: For email (set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`)

**Service Dependencies:**
- Backend requires: PostgreSQL (required)
- Optional: Redis (for rate limiting), RabbitMQ (for async tasks), Elasticsearch (for search)
- WebRTC: Janus Gateway and TURN server (configure `JANUS_URL`, `TURN_SERVER`, `TURNS_SERVER`)

**Note:** The application is designed to work without optional services. Rate limiting, search, and message queuing will be disabled if their respective services are not configured.

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

## WebRTC Configuration (TURN Server)

### TURN Server Setup

A TURN server is essential for WebRTC calls when clients are behind NAT/firewalls. You can use:
- **Self-hosted Coturn**: Deploy on a server with public IP
- **Cloud TURN services**: Twilio, Metered, or other TURN-as-a-Service providers

**Configuration:**
Set the following in your `.env` file:
```bash
TURN_SERVER=turn:your-turn-server.com:3478
TURNS_SERVER=turns:your-turn-server.com:5349
TURN_USERNAME=your_username
TURN_CREDENTIAL=your_password
```

**Production Recommendations:**
1. **Use TURNS (TLS)**: Prefer `turns://` URLs for secure connections
2. **Time-Limited Credentials**: Use HMAC-based tokens instead of static credentials
3. **Multiple TURN Servers**: Configure multiple ICE servers for redundancy
4. **STUN Server**: Google's public STUN server is used by default (`stun:stun.l.google.com:19302`)

**Testing Connectivity:**
```bash
# Test TURN server (requires coturn-utils or similar tool)
turnutils_uclient -v -u your_username -w your_password your-turn-server.com
```

## Troubleshooting

- **Database Connection Refused**: 
  - Verify `SPRING_DATASOURCE_URL` is set correctly in `.env` file
  - Check that your Neon database (or PostgreSQL instance) is accessible
  - Ensure SSL is enabled if required: `?sslmode=require` in the JDBC URL
  - Test connection: `psql "your-connection-string"`

- **JWT Errors**: Verify `JWT_SECRET` is identical in all instances and meets length requirements (32+ bytes).

- **File Upload Failures**: 
  - Check MinIO/S3 configuration and bucket permissions
  - Ensure `MINIO_ENDPOINT` is set correctly in `.env`
  - Verify `AWS_ACCESS_KEY` and `AWS_SECRET_KEY` are correct
  ```bash
  # Check MinIO health (if using local MinIO)
  curl http://localhost:9000/minio/health/live
  
  # Access MinIO console
  # URL: http://localhost:9001
  # Username: healthlink_minio
  # Password: healthlink_minio_password_123
  ```
- **RabbitMQ Connection Issues**: 
  - Verify `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_DEFAULT_USER`, and `RABBITMQ_DEFAULT_PASS` are set correctly
  - Check that RabbitMQ service is accessible from your backend
  - If using cloud RabbitMQ, verify the connection string format

- **WebRTC Call Failures**: 
  - Verify `JANUS_URL` is set correctly (if using Janus Gateway)
  - Check `TURN_SERVER` and `TURNS_SERVER` configuration
  - Test STUN server: `stun:stun.l.google.com:19302` (should always work)
  - Check browser console for ICE candidate gathering errors

- **Rate Limiting Errors (429)**: 
  - Redis is required for Bucket4j rate limiting
  - Set `RATE_LIMIT_ENABLED=true` and configure `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
  - Or disable rate limiting: `RATE_LIMIT_ENABLED=false`

- **Elasticsearch Not Available**: 
  - Set `ELASTICSEARCH_ENABLED=true` and configure `ELASTICSEARCH_URIS`
  - Or disable search features: `ELASTICSEARCH_ENABLED=false` (default)

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
- **Network Security**: Use firewall rules to restrict access to database and internal service ports. Only expose necessary ports (e.g., 8080 for the backend API).
- **Secrets Management**: Do not commit `.env` files or credentials to version control. Use environment-specific secrets injection.

### PHI Compliance (HIPAA-like)

- **Audit Logging**: All PHI access logged via `SafeLogger.audit()` with actor, action, resource, reason.
- **Field-Level Encryption**: Sensitive fields (SSN, CNIC, phone) encrypted at rest with AES-256.
- **Access Control**: RBAC enforced at controller/service layer. Patients cannot access other patients' PHI.
- **Token Revocation**: Forced logout via `tokensRevokedAt` timestamp. Update timestamp to revoke all user tokens immediately.
- **Data Retention**: Implement soft deletes (`deleted_at` timestamp) for GDPR/HIPAA compliance. Hard delete after retention period.
- **Monitoring**: Sentry integration for error tracking (production). Exclude PHI from exception messages.
