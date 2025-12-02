# HealthLink MVP Scope

Based on `requirements.md`, this document outlines the **Minimum Viable Product (MVP)** scope for HealthLink, focusing on core functionality needed for initial launch.

## Core Features (MVP)

### 1. Authentication & User Management ✅
- **User Roles**: Patient, Doctor (Staff, Institution, Admin, Owner can be added later)
- **Self-Signup**: Patient and Doctor registration
- **Email Verification**: OTP-based email verification
- **Login/Logout**: JWT-based authentication with refresh tokens
- **Password Reset**: OTP-based password reset flow

**Status**: ✅ Implemented

### 2. Appointment Management (Core)
- **Appointment Booking**: Patients can book appointments with doctors
- **Appointment Status**: PENDING, CONFIRMED, COMPLETED, CANCELLED
- **Basic Scheduling**: Doctor availability and slot management
- **Appointment List**: View upcoming and past appointments

**Status**: ⚠️ Partially implemented (needs testing)

### 3. Medical Records (Basic)
- **Medical History**: Patients can view their medical history
- **Record Creation**: Doctors/Staff can create medical records
- **PHI Encryption**: At-rest encryption for medical records

**Status**: ⚠️ Partially implemented (needs testing)

### 4. Doctor Profile Management
- **Doctor Registration**: Self-signup with PMDC verification
- **Profile Management**: Update doctor profile, specialization, bio
- **Clinic Management**: Create and manage clinics (basic)

**Status**: ⚠️ Partially implemented

## Deferred Features (Post-MVP)

### ❌ Not in MVP
1. **Telehealth/Video Calls**: Defer Janus WebRTC integration
2. **Payment Processing**: Defer manual payment verification and dispute resolution
3. **Advanced Search**: Defer Elasticsearch-based doctor discovery
4. **Analytics Dashboards**: Defer revenue/rating analytics
5. **Push Notifications**: Defer Firebase push notifications
6. **Emergency Patient Workflow**: Defer emergency patient registration
7. **Family Medical Tree**: Defer recursive family medical history
8. **Consent Forms**: Defer digital consent management
9. **Staff Management**: Defer staff invitation and management
10. **Institution Management**: Defer organization/institution features
11. **Admin/Owner Dashboards**: Defer admin approval workflows

## Technical Stack (MVP)

### Backend
- ✅ Spring Boot 3 + Java 21
- ✅ PostgreSQL (Neon)
- ✅ JWT Authentication
- ✅ PHI Encryption (AES-256-GCM)
- ❌ Redis (optional for MVP - OTP can work without it)
- ❌ Elasticsearch (not needed for MVP)
- ❌ MinIO/SeaweedFS (can use local storage for MVP)
- ❌ RabbitMQ (not needed for MVP)
- ❌ Janus WebRTC (not needed for MVP)

### Frontend (Web)
- ✅ Next.js 14 + TypeScript
- ✅ Patient Dashboard
- ✅ Doctor Dashboard
- ⚠️ Admin Dashboard (basic only)

### Mobile App (Flutter)
- ⚠️ Use OpenAPI Generator for DTOs
- ⚠️ Basic authentication flow
- ⚠️ Appointment booking
- ⚠️ Medical records viewing

## API Endpoints (MVP)

### Authentication ✅
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/otp/send` - Send OTP
- `POST /api/v1/auth/email/verify` - Verify email with OTP
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/logout` - Logout

### Appointments ⚠️
- `GET /api/v1/appointments` - List appointments
- `POST /api/v1/appointments` - Book appointment
- `GET /api/v1/appointments/{id}` - Get appointment details
- `PUT /api/v1/appointments/{id}` - Update appointment
- `DELETE /api/v1/appointments/{id}` - Cancel appointment

### Medical Records ⚠️
- `GET /api/v1/medical-records` - List medical records
- `POST /api/v1/medical-records` - Create medical record
- `GET /api/v1/medical-records/{id}` - Get medical record
- `PUT /api/v1/medical-records/{id}` - Update medical record

### Doctor Profile ⚠️
- `GET /api/v1/doctors/{id}` - Get doctor profile
- `PUT /api/v1/doctors/{id}` - Update doctor profile
- `GET /api/v1/doctors/{id}/clinics` - List doctor clinics
- `POST /api/v1/doctors/{id}/clinics` - Create clinic

## MVP Success Criteria

1. ✅ Patient can register and verify email
2. ✅ Patient can log in
3. ⚠️ Patient can book an appointment with a doctor
4. ⚠️ Doctor can view appointments
5. ⚠️ Patient can view medical history
6. ⚠️ Doctor can create medical records
7. ⚠️ Basic error handling and user-friendly messages

## Next Steps

1. **Test Core Flows**: Verify registration, login, appointment booking
2. **Fix Known Issues**: Dashboard redirect, token refresh
3. **Simplify Dependencies**: Make Redis, Elasticsearch, MinIO optional
4. **Mobile App Setup**: Use OpenAPI generator for Flutter DTOs
5. **Documentation**: Update API docs to match implementation

## Notes

- **Time Constraint**: Focus on MVP features only
- **Scalability**: Design for future features but don't implement yet
- **Security**: Maintain HIPAA compliance even in MVP
- **Testing**: Prioritize manual testing of core user flows

