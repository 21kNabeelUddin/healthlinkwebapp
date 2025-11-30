# HealthLink - Streamlined Feature Specification

**Version:** 1.0 (Production-Ready Core Features)  
**Date:** November 24, 2025  
**Focus:** Essential healthcare platform features only
**Rule** Use latest version of everything as of november, 2025 after searching online
**Rule** Use java 21 LTS
---

## Technology Stack (Latest Stable LTS - Nov 2025)

### Backend

- **Java:** 21 LTS (Latest release)
- **Spring Boot:** 3.5.8
- **PostgreSQL:** 18.1
- **Redis:** 8.4.0
- **Elasticsearch:** 9.2.1
- **RabbitMQ:** 4.2.1
- **Gradle:** 9.2.1
- **Docker:** 29.0.2
- **Kubernetes:** 1.34.2

### Mobile

- **Flutter:** 3.38 (Dart 3.10)
- **Platforms:** Android, iOS (no web deployment)

### Self-Hosted Services

- MinIO (S3-compatible storage)
- Janus (WebRTC server)
- SMTP server (email)
- Elasticsearch (search)
- Firebase (crash reporting, push notifications)

---

## Core Features

### 1. Authentication & Access ✅ **NECESSARY**

- JWT authentication (15-min access tokens + 7-day refresh with rotation)
- Password-based registration and login for **Patients** with 2fa support through email
- Password-based login for **Doctors, Staff, Organizations, Admins, Platform Owner** with 2fa support through email
- Admin login via username/password only
- Pending approval → forced logout for doctor/org
- Multi-language support: **Urdu/English + RTL**
- Session management with Redis

**Security:**

- BCrypt password hashing (strength 12)
- Rate limiting (5 OTP/hour per email)
- Token blacklisting on logout
- RBAC enforcement

---

### 2. Roles & Permissions ✅ **NECESSARY**

#### **Patient**

- Book appointments
- Pay (manual verification)
- View appointment history
- Manage medical records
- Upload lab results
- View prescriptions
- Submit reviews and ratings

#### **Doctor**

- Manage practice(s)
- Manage staff assignment
- Manage services offered at owned facilities
- Manage schedules for owned practices
- Manage appointments
- **Set payment account details** (per doctor or practice-level)
- PMDC license verification required
- Adjust appointment settings (early check-in only, NO extensions/buffers)
- Video calls (WebRTC)
- Create prescriptions with drug interaction warnings
- Add notes and lab test orders
- **Verify patient payments** (approve/reject payment receipts)
- View analytics: **revenue, reviews**

#### **Staff**

- Added by Doctor/Organization only
- Assigned to specific facilities
- Patient registration
- Manage bookings
- **Verify patient payments** (approve/reject payment receipts)
- Check-in management
- Limited PHI access (name/contact only unless permitted)
- Participate in remote video calls (optional, as external support with IoT devices)

#### **Organization**

- Registration with **valid Pakistan Org Number**
- Provide **official organization email** on signup
- Approval by Admin via portal (rejection/approval communicated via email)
- Manage network doctors
- Manage staff
- Manage facilities
- **Manage services offered at facilities**
- **Manage schedules for services**
- Access PHI within network only
- **Configure payment settings:**
  - Allow per-doctor payment account configuration OR
  - Set payment accounts for each doctor centrally

#### **Admin**

- Created by Platform Owner
- **Approve/reject doctors and organizations via portal**
- Approval/rejection communicated to users via email automatically
- View analytics (**NO PHI** - anonymized only)
- **Handle payment dispute escalations** (from practice owners)
- **Final escalation for unresolved payment disputes**

#### **Platform Owner**

- Manage Admin accounts
- System-wide analytics (NO PHI)
---

### 3. Doctor Workflow ✅ **NECESSARY**

**Registration:**

- PMDC ID required (format validation: `12345-P`)
- PMDC verification by Admin/Organization
- License document upload

**Practice Management:**

- Create/manage practice(s)
- Create/manage facility(-ies)
- **Configure services offered at each facility**
- Assign staff to facilities
- **Set payment account details:**
  - Option 1: Doctor sets own account
  - Option 2: Organization sets per-doctor accounts
  - Display to patients during payment

**Schedule Management:**

- Create weekly schedule templates
- Manage availability slots
- **Manage schedules for owned practices**
- Set appointment duration (default: 15 minutes)
- Allow early check-in (configurable minutes, default: 10)
- ❌ NO extensions, NO buffers for appointments

**Appointment Management:**

- View upcoming/past appointments
- Initiate video calls (after check-ins complete)
- Add notes during/after consultation
- Create prescriptions
- Order lab tests
- Early check-in approval

**Analytics:**

- **Revenue tracking** (total, per period)
- **Average rating and review count**

---

### 4. Patient Workflow ✅ **NECESSARY**

**Registration & Authentication:**

- Email and password registration then otp verification
- 2fa can be enabled from account with only email otp support
- Auto-approved upon registration

**Appointment Booking:**

- Search doctors by specialty, location, rating, availability
- View doctor profile (qualifications, experience, fee, ratings)
- Select available time slot
- Prevent overlapping bookings (one active appointment at a time)

**Payment Flow (Manual Verification):**

1. Patient selects payment method: **Mobile Payment** or **Cash**
2. **If Mobile Payment:**
   - System displays payment account details:
     - If per-doctor configured: Doctor's account
     - Otherwise: Practice owner's designated account for that doctor
   - Patient transfers funds externally
   - **Patient uploads payment receipt/screenshot** in app
   - Receipt visible to:
     - Staff (with notification)
     - Doctor (no notification, can view manually)
   - **Staff OR Doctor verifies payment:**
     - Approve → Appointment confirmed
     - Reject → Payment disputed
3. **If Cash:**
   - Appointment tentatively scheduled
   - Patient pays at facility
   - Staff records cash payment in app
   - Appointment confirmed

**Payment Dispute Resolution:**

- If verifier rejects but patient contests:
  1. Issue escalated to **Practice Owner**
  2. If unresolved, escalated to **Admin**
  3. If still unresolved, parties advised to resolve legally

**Appointment Management:**

- View upcoming appointments
- **Reschedule** (if new slots available)
- ❌ NO waitlist
- ❌ NO open-slot alerts
- Cancel appointments (no refund for cancellation by patient, manual dispute if needed. Reschedule option if cancelled by doctor, manual dispute for refund if not rescheduling.)
- Check-in before appointment

**Medical Records:**

- View prescriptions
- **Upload and attach lab results to appointments**
- View medical history
- Export records as PDF
- **Manage family medical tree** (hereditary conditions tracking)

**Reviews:**

- Rate doctor after completed appointment
- Write review

**Notifications:**

- **In-app, and push notifications** (NO email, NO SMS)
- **Configurable reminders:** User sets preferred reminder times (default: 1h, 15m, 5m before appointment)
- Payment verification status
- Appointment confirmations
- Cancellations

---

### 5. Staff Workflow ✅ **NECESSARY**

**Registration:**

- Added by Doctor or Organization only
- Assigned to specific facility (Can be assigned to multiple facilities)
- Approved on addition by doctor, no self-registration, only login
- credentials generated by doctor with staff's own email, password resettable after account creation on login, otp verification through email required for successful registration
- Email can be used for 2fa

**Responsibilities:**

- Register walk-in patients
- Manage appointment bookings
- Patient check-in
- **Verify mobile payments:**
  - Receive notification for uploaded receipts
  - Approve/reject payments
- Record cash payments
- **For remote appointments:** Optionally join video call with IoT devices (external camera/mic at patient's home)

**Permissions:**

- Limited PHI visibility (name, contact only unless granted by doctor/org)
- Cannot view full medical history unless permitted
- For remote appointments, where assigned, can view patient's phone number and address

**Availability Toggle:**

- Mark self as available/unavailable for assignments

---

### 6. Organization Workflow ✅ **NECESSARY**

**Registration:**

- Organization name
- Pakistan Organization Number (7-digit validation)
- **Official organization email address**
- Admin approval required

**Approval Process:**

- Admin reviews via portal
- Approval/rejection email sent to **official organization email**

**Management:**

- Onboard doctors to network
- Onboard staff
- Create/manage facilities
- **Define services offered at each facility**
- **Create and manage service schedules**
- **Configure payment accounts:**
  - Allow doctors to set own accounts, OR
  - Centrally set payment accounts for each doctor
- Access PHI within network
- **Handle payment disputes** escalated by staff/doctors

---

### 7. Admin & Platform Owner ✅ **NECESSARY**

**Admin Responsibilities:**

- Created by Platform Owner
- **Approve/reject doctors** (PMDC verification)
- **Approve/reject organizations** (Pakistan Org Number verification)
- Send automated approval/rejection emails
- View **anonymized analytics** (NO PHI)
- **Resolve payment disputes** escalated from organizations

**Platform Owner Responsibilities:**

- Create/manage Admin accounts
- View system-wide analytics (NO PHI)

---

### 8. Appointment Management ✅ **NECESSARY**

**Booking:**

- One confirmed appointment per patient per slot
- Slot selection from doctor's schedule
- Payment process initiated (manual verification)
- Appointment status: PENDING → CONFIRMED (after payment) → IN_PROGRESS → COMPLETED/CANCELED

**Check-Ins:**

- **Patient check-in** required (early check-in)
- **Staff check-in** required for remote appointments where staff assigned
- ❌ NO automatic extensions
- ❌ NO buffer time

**Rescheduling:**

- Patient can reschedule if open slots available
- ❌ NO automatic reschedule invitations
- ❌ NO waitlist

**Recurring Appointments:**

- Not implemented, out of scope

---

### 9. Video Call Integration ✅ **NECESSARY**

**Technology:**

- Self-hosted **Janus WebRTC Gateway**

**Call Flow:**

1. All participants check in
2. **Doctor initiates call** (only doctor can start)
3. **Participants:**
   - Doctor (required)
   - Patient (required)
   - Staff (optional, for remote consultations with IoT device support, if assigned)
4. Doctor can add notes, create prescriptions, and lab tests recommendations during call
5. Doctor ends call

**IoT Device Support:**

- Staff brings external camera/microphone to patient's location (for remote home visits)
- Devices connected as external peripherals during video call from staff participant

**Consent:**

- Digital consent required before video calls
- Stored securely with versioning

---

### 10. Prescription & Lab Test Management ✅ **NECESSARY**

**Prescriptions:**

- Doctor creates prescription during appointment
- Templates available for common prescriptions
- **Drug interaction warnings** via **OpenFDA API** integration
- Track patient's current medications for cross-checking
- **Prescription includes:**
  - Practice name
  - Facility name
  - Doctor name
  - Date and time
  - Medications with dosage
  - ❌ NO digital e-signature (removed - extra)

**Lab Tests:**

- Doctor orders lab tests
- **Patient uploads lab results** via app
- **Results can be attached to appointment** (can be new or recurring appointment , self-scheduled not automatic, if needed)
- ❌ NO bi-directional lab integration

---

### 11. Notifications ✅ **NECESSARY**

**Channels:**

- **In-app and push** (NO email for notifications, NO SMS)

**Types:**

- **Appointment reminders** (configurable by user - default: 1h, 15m, 5m before)
- Payment verification status
- Appointment confirmations
- Cancellations
- Check-in reminders

**Configuration:**

- Users set preferred reminder times
- Enable/disable specific notification types

---

### 12. Analytics & Reporting ✅ **NECESSARY (Limited)**

**Doctor Analytics:**

- **Total revenue** (appointments, payments)
- **Average rating**
- **Total review count**
- Appointment count statistics

**Patient Analytics:**

- Appointment history
- Total payments
- Visited doctors

**Organization Analytics:**

- Staff/Doctor/Facility/Service performance
- Network size (doctors, staff, facilities)

**Admin Analytics:**

- **Anonymized system metrics** (NO PHI)
- Total users by role
- Total appointments
- Platform usage statistics

**PHI Access Logs:**

- Track who accessed PHI, when, and why
- Audit trail for compliance

---

### 13. Search & Discovery ✅ **NECESSARY**

**Elasticsearch Integration:**

- Fast doctor search
- Filters:
  - Specialty
  - Location (city/area)
  - Rating (minimum)
  - Availability (next available slot)
  - Organization
- Sort by rating, availability
- ❌ NO map view

---

### 14. Payment System ✅ **NECESSARY (Manual Verification)**

**Payment Methods:**

1. **Mobile Payment (Bank Transfer/Mobile Money):**

   - Patient shown account details (doctor or practice-configured)
   - Patient transfers externally (JazzCash, EasyPaisa, bank transfer, etc.)
   - Patient uploads receipt/screenshot
   - Staff or Doctor verifies
   - Approval = Booking confirmed
   - Rejection = Dispute flow

2. **Cash Payment:**
   - Patient pays at facility
   - Staff records payment in app
   - Booking confirmed

**Dispute Resolution:**

- Patient contests rejection
- Escalation: Staff/Doctor → Practice Owner → Admin → Legal

**Refund Policy:**

- ❌ NO automatic refunds
- If doctor/staff cancels: Manual resolution
- Custom refund terms not displayed (handled case-by-case)

**Configuration:**

- **Doctor-level:** Doctor sets own payment account
- **Organization-level:** Organization sets accounts for doctors
- **Hybrid:** Organization allows per-doctor configuration

---

### 15. Medical Records ✅ **NECESSARY**

**Storage:**

- Self-hosted **MinIO** (S3-compatible)
- Encrypted at rest

**Features:**

- Secure upload/view (audit-logged)
- Export as PDF
- **Structured medical history:**
  - Allergies
  - Vaccinations
  - Chronic illnesses
  - **Family medical tree** (hereditary tracking - IMPLEMENT IN MOBILE APP)
- PHI access audit trail

**Lab Results:**

- Patient uploads reports
- Attached to appointments
- Doctor reviews and adds notes

---

### 16. Consent & Legal ✅ **NECESSARY**

**Digital Consent Forms:**

- Required before remote appointment booking
- Available in Urdu and English
- Versioned (track consent acceptance)
- Stored securely

**Consent Verification:**

- Checked before remote appointment booking
- Cannot register appointment without valid consent

---

### 17. Webhooks & Integrations ✅ **NECESSARY**

**Outgoing Webhooks:**

- `appointment.created`
- `appointment.canceled`
- `payment.verified`
- `payment.disputed`

**Use Cases:**

- CRM integration
- Analytics tools
- Custom notifications

---

## Essential DevOps & Deployment

### Infrastructure (Self-Hosted)

- Docker Compose for local development
- Kubernetes manifests for production
- PostgreSQL 18.1
- Redis 8.4.0
- Elasticsearch 9.2.1
- RabbitMQ 4.2.1
- MinIO (storage)
- Janus (WebRTC)
- SMTP server

### Monitoring (Minimal)

- **Prometheus** (metrics collection)
- **Grafana** (dashboards - optional)
- **Logging:** SLF4J + Logback (NO PHI in logs)
- **Crash Reporting:** Firebase Crashlytics (mobile)

### Security

- HTTPS/TLS (production)
- **Secrets Management:** Open-source solution (e.g., **Vault** by HashiCorp - self-hosted)
- JWT tokens with rotation
- Password hashing (BCrypt)
- OTP rate limiting
- RBAC enforcement
- PHI audit logging
- Soft delete for GDPR compliance

### CI/CD

- Using github actions

### Backups

- PostgreSQL automated backups
- MinIO bucket replication (optional)

---