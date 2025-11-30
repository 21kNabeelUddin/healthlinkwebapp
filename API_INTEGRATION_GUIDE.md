# HealthLink API Integration Guide

## Table of Contents
1. [Authentication](#authentication)
2. [Core Endpoints](#core-endpoints)
3. [WebRTC Video Calls](#webrtc-video-calls)
4. [File Uploads](#file-uploads)
5. [Webhook Events](#webhook-events)
6. [Error Handling](#error-handling)
7. [Rate Limiting](#rate-limiting)
8. [Testing](#testing)

---

## Authentication

HealthLink uses **JWT (JSON Web Token)** authentication with short-lived access tokens and rotating refresh tokens.

### Login Flow

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "patient@example.com",
  "password": "SecurePassword123!",
  "role": "PATIENT"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900000,
  "tokenType": "Bearer",
  "user": {
    "id": 123,
    "email": "patient@example.com",
    "role": "PATIENT",
    "name": "John Doe"
  }
}
```

### Using Access Tokens

Include the access token in the `Authorization` header for all authenticated requests:

```http
GET /api/v1/appointments
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Token Refresh

Access tokens expire after **15 minutes**. Use the refresh token to obtain a new access token:

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900000,
  "tokenType": "Bearer"
}
```

**Error (401 Unauthorized):**
```json
{
  "timestamp": "2025-11-26T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired refresh token",
  "path": "/api/v1/auth/refresh"
}
```

### Logout

Revoke the current refresh token:

```http
POST /api/v1/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (204 No Content)**

### Forced Logout (Admin)

Administrators can force logout all sessions for a user by updating the `tokensRevokedAt` timestamp:

```http
POST /api/v1/admin/users/{userId}/revoke-tokens
Authorization: Bearer <admin_access_token>
```

All tokens issued before the revocation timestamp will be invalidated.

---

## Core Endpoints

### Appointments

#### List Appointments

```http
GET /api/v1/appointments?status=SCHEDULED&page=0&size=20
Authorization: Bearer <access_token>
```

**Query Parameters:**
- `status` (optional): Filter by status (`SCHEDULED`, `COMPLETED`, `CANCELLED`)
- `page` (default: 0): Page number (0-indexed)
- `size` (default: 20): Page size

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 456,
      "patientId": 123,
      "doctorId": 789,
      "facilityId": 101,
      "scheduledAt": "2025-11-26T14:00:00Z",
      "status": "SCHEDULED",
      "consultationType": "VIDEO_CALL",
      "chiefComplaint": "Fever and headache",
      "meetingLink": "https://healthlink.com/call/456"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 5,
  "totalPages": 1
}
```

#### Create Appointment

```http
POST /api/v1/appointments
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "patientId": 123,
  "doctorId": 789,
  "facilityId": 101,
  "scheduledAt": "2025-11-26T14:00:00Z",
  "consultationType": "VIDEO_CALL",
  "chiefComplaint": "Fever and headache for 3 days"
}
```

**Response (201 Created):**
```json
{
  "id": 456,
  "patientId": 123,
  "doctorId": 789,
  "facilityId": 101,
  "scheduledAt": "2025-11-26T14:00:00Z",
  "status": "SCHEDULED",
  "consultationType": "VIDEO_CALL",
  "chiefComplaint": "Fever and headache for 3 days",
  "meetingLink": "https://healthlink.com/call/456",
  "createdAt": "2025-11-25T10:30:00Z"
}
```

**Validation Errors (400 Bad Request):**
```json
{
  "timestamp": "2025-11-26T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Doctor is not available at the requested time",
  "path": "/api/v1/appointments"
}
```

#### Cancel Appointment

```http
DELETE /api/v1/appointments/{id}
Authorization: Bearer <access_token>
```

**Response (204 No Content)**

### Medical Records

#### Get Patient Records

```http
GET /api/v1/records?patientId=123&page=0&size=20
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 789,
      "patientId": 123,
      "doctorId": 456,
      "appointmentId": 101,
      "diagnosis": "Upper respiratory tract infection",
      "treatment": "Rest, fluids, paracetamol 500mg q6h",
      "notes": "Follow-up in 5 days if symptoms persist",
      "recordDate": "2025-11-20T10:00:00Z",
      "fileUrls": [
        "https://healthlink.s3.amazonaws.com/records/789_xray.pdf"
      ]
    }
  ],
  "totalElements": 15,
  "totalPages": 1
}
```

**RBAC Rules:**
- **Patients**: Can only access their own records.
- **Doctors**: Can access records for patients they've treated.
- **Staff**: Can access records for patients at their facility.
- **Admins**: Full access.

### Prescriptions

#### Create Prescription

```http
POST /api/v1/prescriptions
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "patientId": 123,
  "appointmentId": 456,
  "medications": [
    {
      "name": "Paracetamol",
      "dosage": "500mg",
      "frequency": "Every 6 hours",
      "duration": "5 days",
      "instructions": "Take with food"
    }
  ],
  "instructions": "Rest and stay hydrated",
  "validUntil": "2025-12-26T23:59:59Z"
}
```

**Response (201 Created):**
```json
{
  "id": 789,
  "patientId": 123,
  "doctorId": 456,
  "appointmentId": 456,
  "medications": [
    {
      "id": 101,
      "name": "Paracetamol",
      "dosage": "500mg",
      "frequency": "Every 6 hours",
      "duration": "5 days",
      "instructions": "Take with food"
    }
  ],
  "instructions": "Rest and stay hydrated",
  "warnings": [
    {
      "type": "ALLERGY",
      "severity": "HIGH",
      "message": "Patient has documented allergy to NSAIDs"
    }
  ],
  "validUntil": "2025-12-26T23:59:59Z",
  "createdAt": "2025-11-26T10:30:00Z",
  "qrCode": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
}
```

**Drug Interaction Warnings:**
The system automatically checks for drug interactions using the OpenFDA API and patient allergy history. Warnings are included in the response.

---

## WebRTC Video Calls

### Get WebRTC Token

Before starting a video call, obtain a WebRTC token that includes ICE server configuration:

```http
POST /api/v1/video-calls/token
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "appointmentId": 456,
  "participantRole": "PATIENT"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600000,
  "iceServers": [
    {
      "urls": "stun:stun.l.google.com:19302"
    },
    {
      "urls": "turn:coturn:3478",
      "username": "healthlink",
      "credential": "healthlink_turn_secret_2025"
    },
    {
      "urls": "turns:coturn:5349",
      "username": "healthlink",
      "credential": "healthlink_turn_secret_2025"
    }
  ],
  "signalingUrl": "ws://localhost:8080/signaling",
  "janusUrl": "http://localhost:8088/janus",
  "roomId": "456"
}
```

### WebRTC Signaling Flow

1. **Connect to Signaling Server**: Use the `signalingUrl` from the token response.
2. **Join Room**: Send a `join` message with the `roomId` and `token`.
3. **Exchange SDP**: Use the signaling server to exchange SDP offers/answers and ICE candidates.
4. **Establish Peer Connection**: Create a WebRTC PeerConnection with the provided ICE servers.

**Example WebSocket Messages:**

```javascript
// Client → Server: Join room
{
  "type": "join",
  "roomId": "456",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

// Server → Client: User joined
{
  "type": "user-joined",
  "userId": 789,
  "role": "DOCTOR"
}

// Client → Server: SDP offer
{
  "type": "offer",
  "roomId": "456",
  "sdp": {
    "type": "offer",
    "sdp": "v=0\r\no=- 123456789 2 IN IP4 127.0.0.1\r\n..."
  }
}

// Server → Client: SDP answer
{
  "type": "answer",
  "fromUserId": 789,
  "sdp": {
    "type": "answer",
    "sdp": "v=0\r\no=- 987654321 2 IN IP4 127.0.0.1\r\n..."
  }
}

// Client → Server: ICE candidate
{
  "type": "ice-candidate",
  "roomId": "456",
  "candidate": {
    "candidate": "candidate:1 1 UDP 2130706431 192.168.1.100 54321 typ host",
    "sdpMid": "0",
    "sdpMLineIndex": 0
  }
}
```

### End Video Call

```http
POST /api/v1/video-calls/{appointmentId}/end
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "appointmentId": 456,
  "startedAt": "2025-11-26T14:00:00Z",
  "endedAt": "2025-11-26T14:30:00Z",
  "duration": 1800,
  "participants": [
    {
      "userId": 123,
      "role": "PATIENT",
      "joinedAt": "2025-11-26T14:00:15Z",
      "leftAt": "2025-11-26T14:30:00Z"
    },
    {
      "userId": 789,
      "role": "DOCTOR",
      "joinedAt": "2025-11-26T14:00:10Z",
      "leftAt": "2025-11-26T14:30:00Z"
    }
  ]
}
```

---

## File Uploads

### Upload File (Medical Image/Document)

```http
POST /api/v1/files/upload
Authorization: Bearer <access_token>
Content-Type: multipart/form-data

file: <binary_data>
contentType: image/png
fileName: xray_chest.png
```

**Supported Content Types:**
- Images: `image/png`, `image/jpeg`, `image/gif`
- Documents: `application/pdf`

**Size Limit:** 5 MB per file

**Response (200 OK):**
```json
{
  "fileId": "abc123-def456-ghi789",
  "fileName": "xray_chest.png",
  "contentType": "image/png",
  "size": 1048576,
  "uploadedAt": "2025-11-26T10:30:00Z",
  "url": "https://healthlink.s3.amazonaws.com/records/abc123-def456-ghi789"
}
```

### Download File (Presigned URL)

```http
GET /api/v1/files/{fileId}/download
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "fileId": "abc123-def456-ghi789",
  "presignedUrl": "https://healthlink.s3.amazonaws.com/records/abc123-def456-ghi789?X-Amz-Expires=3600&...",
  "expiresIn": 3600
}
```

**Validation Errors (400 Bad Request):**
```json
{
  "timestamp": "2025-11-26T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "File size exceeds 5MB limit",
  "path": "/api/v1/files/upload"
}
```

---

## Webhook Events

HealthLink emits webhook events for key actions. Subscribe to webhooks via the admin panel.

### Webhook Subscription

```http
POST /api/v1/webhooks/subscribe
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "url": "https://your-server.com/webhooks/healthlink",
  "events": [
    "appointment.created",
    "appointment.cancelled",
    "prescription.created",
    "payment.completed"
  ],
  "secret": "your_webhook_secret"
}
```

### Webhook Event Structure

All webhook events follow this structure:

```json
{
  "id": "evt_abc123def456",
  "type": "appointment.created",
  "timestamp": "2025-11-26T10:30:00Z",
  "data": {
    "appointmentId": 456,
    "patientId": 123,
    "doctorId": 789,
    "scheduledAt": "2025-11-26T14:00:00Z",
    "status": "SCHEDULED"
  },
  "signature": "sha256=abc123def456..."
}
```

### Webhook Signature Verification

Verify webhook authenticity using HMAC-SHA256:

```python
import hmac
import hashlib

def verify_webhook(payload, signature, secret):
    computed_signature = hmac.new(
        secret.encode(),
        payload.encode(),
        hashlib.sha256
    ).hexdigest()
    return hmac.compare_digest(f"sha256={computed_signature}", signature)
```

### Event Types

| Event | Description |
|-------|-------------|
| `appointment.created` | New appointment scheduled |
| `appointment.updated` | Appointment details changed |
| `appointment.cancelled` | Appointment cancelled |
| `appointment.completed` | Appointment marked complete |
| `prescription.created` | New prescription issued |
| `payment.completed` | Payment successfully processed |
| `payment.failed` | Payment failed |
| `user.created` | New user registered |

---

## Error Handling

### Standard Error Response

```json
{
  "timestamp": "2025-11-26T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for field 'email': must be a valid email address",
  "path": "/api/v1/auth/register",
  "traceId": "abc123def456"
}
```

### HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | OK | Successful request |
| 201 | Created | Resource created (appointment, prescription) |
| 204 | No Content | Successful delete or logout |
| 400 | Bad Request | Validation errors, malformed JSON |
| 401 | Unauthorized | Invalid or expired token |
| 403 | Forbidden | Insufficient permissions (RBAC) |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Doctor double-booking, duplicate email |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Unhandled server error |

### Rate Limiting

```json
{
  "timestamp": "2025-11-26T10:30:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded: 100 requests per minute",
  "path": "/api/v1/appointments",
  "retryAfter": 45
}
```

**Headers:**
- `X-Rate-Limit-Remaining`: Requests remaining in current window
- `X-Rate-Limit-Retry-After-Seconds`: Seconds until rate limit resets

---

## Rate Limiting

HealthLink uses **Bucket4j** with Redis-backed rate limiting to prevent abuse.

**Default Limits:**
- **General API**: 100 requests/minute per user
- **Authentication**: 10 requests/minute per IP (login, register)
- **File Uploads**: 10 requests/minute per user

**Headers:**
```http
X-Rate-Limit-Remaining: 95
X-Rate-Limit-Retry-After-Seconds: 0
```

**Rate Limit Exceeded (429 Too Many Requests):**
```json
{
  "timestamp": "2025-11-26T10:30:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded: 100 requests per minute",
  "retryAfter": 45
}
```

**Bypass Rate Limiting (Development):**
Set `healthlink.rate-limit.enabled=false` in `application-test.yml`.

---

## Testing

### Swagger UI

Interactive API documentation available at:
**http://localhost:8080/swagger-ui.html**

### OpenAPI Spec (JSON)

Import the OpenAPI spec into Postman/Insomnia:
**http://localhost:8080/api-docs**

### Postman Collection

1. Import OpenAPI spec from `http://localhost:8080/api-docs`
2. Set environment variables:
   - `BASE_URL`: `http://localhost:8080`
   - `ACCESS_TOKEN`: Obtained from login response
3. Run authentication flow to obtain tokens
4. Test endpoints with bearer token authentication

### Mock PHI Data

**Test Credentials:**
- **Patient**: patient@test.com / Test123!
- **Doctor**: doctor@test.com / Test123!
- **Admin**: admin@test.com / Test123!

**Never use real PHI (Protected Health Information) in tests or logs.**

---

## Additional Resources

- [README](README.md): Project overview
- [Deployment Guide](healthlink_backend/DEPLOYMENT_GUIDE.md): Production deployment
- [Coturn Setup Guide](COTURN_DEPLOYMENT_GUIDE.md): TURN server configuration
- [Database Schema](#): Generated entity documentation (pending)

For support, contact: support@healthlink.com
