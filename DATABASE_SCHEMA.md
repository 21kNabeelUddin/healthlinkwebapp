# HealthLink Database Schema Documentation

## Overview

HealthLink uses **PostgreSQL 18.1** as the primary database with **Hibernate JPA** for ORM. The schema supports a healthcare SaaS platform with multi-tenancy, role-based access control (RBAC), and PHI (Protected Health Information) encryption.

**Key Features:**
- Field-level encryption for sensitive data (AES-256)
- Soft deletes for GDPR compliance (`deleted_at` timestamps)
- Audit logging for all PHI access
- Comprehensive indexing for performance
- Liquibase migrations for schema versioning

---

## Core Tables

### 1. Users Table (`users`)

**Purpose:** Central user table supporting multiple roles (Patient, Doctor, Staff, Organization, Admin, Platform Owner).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique user identifier |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | User email (login credential) |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt hashed password |
| `role` | VARCHAR(50) | NOT NULL | User role (PATIENT, DOCTOR, STAFF, ORGANIZATION, ADMIN, PLATFORM_OWNER) |
| `name` | VARCHAR(255) | NOT NULL | Full name |
| `phone` | VARCHAR(50) | ENCRYPTED | Phone number (AES-256 encrypted) |
| `cnic` | VARCHAR(50) | ENCRYPTED, UNIQUE | National ID (Pakistan CNIC) |
| `address` | TEXT | | Physical address |
| `date_of_birth` | DATE | | Birth date |
| `gender` | VARCHAR(20) | | Gender (MALE, FEMALE, OTHER) |
| `is_verified` | BOOLEAN | DEFAULT FALSE | Email verification status |
| `tokens_revoked_at` | TIMESTAMP | | Forced logout timestamp (all tokens before this are invalid) |
| `deleted_at` | TIMESTAMP | | Soft delete timestamp (GDPR compliance) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Account creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update timestamp |

**Indexes:**
- `idx_users_email` (email)
- `idx_users_role` (role)
- `idx_users_deleted_at` (deleted_at) — for soft delete queries

**Role-Specific Columns (Discriminator):**
- **Patient**: `blood_type`, `allergies` (encrypted), `emergency_contact` (encrypted)
- **Doctor**: `license_number`, `specialty`, `pmdc_verified`, `pmdc_license_number`, `years_of_experience`
- **Staff**: `position`, `facility_id` (FK to facilities)
- **Organization**: `org_number`, `org_type`, `tax_id`

**Relationships:**
- One-to-Many with `appointments` (patient_id, doctor_id)
- One-to-Many with `medical_records`
- One-to-Many with `prescriptions`
- One-to-Many with `payments`
- One-to-Many with `notifications`

---

### 2. Appointments Table (`appointments`)

**Purpose:** Manages appointment scheduling, conflict detection, and consultation types.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique appointment identifier |
| `patient_id` | BIGINT | NOT NULL, FK (users.id) | Patient reference |
| `doctor_id` | BIGINT | NOT NULL, FK (users.id) | Doctor reference |
| `facility_id` | BIGINT | FK (facilities.id) | Facility reference (optional for video calls) |
| `staff_id` | BIGINT | FK (users.id) | Staff participant (3-way calls) |
| `scheduled_at` | TIMESTAMP | NOT NULL | Appointment date/time |
| `duration_minutes` | INT | NOT NULL, DEFAULT 30 | Appointment duration |
| `status` | VARCHAR(50) | NOT NULL | Status (SCHEDULED, COMPLETED, CANCELLED, NO_SHOW) |
| `consultation_type` | VARCHAR(50) | NOT NULL | Type (VIDEO_CALL, IN_PERSON, PHONE) |
| `chief_complaint` | TEXT | | Patient's primary concern |
| `meeting_link` | VARCHAR(500) | | WebRTC meeting link |
| `cancellation_reason` | TEXT | | Reason for cancellation |
| `cancelled_by` | BIGINT | FK (users.id) | User who cancelled |
| `payment_id` | BIGINT | FK (payments.id) | Associated payment |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update timestamp |

**Indexes:**
- `idx_appointments_patient_id` (patient_id)
- `idx_appointments_doctor_id` (doctor_id)
- `idx_appointments_scheduled_at` (scheduled_at)
- `idx_appointments_status` (status)
- **Composite Index**: `idx_appointments_doctor_scheduled` (doctor_id, scheduled_at) — for overlap detection

**Business Rules:**
- **Overlap Prevention**: No two appointments for the same doctor can overlap in time.
- **Cancellation Policy**: Appointments can be cancelled up to 2 hours before scheduled time.
- **Staff Participation**: `staff_id` optional; used for 3-way video calls (doctor-patient-staff).

---

### 3. Medical Records Table (`medical_records`)

**Purpose:** Stores encrypted medical records, diagnoses, treatments, and associated files.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique record identifier |
| `patient_id` | BIGINT | NOT NULL, FK (users.id) | Patient reference |
| `doctor_id` | BIGINT | NOT NULL, FK (users.id) | Doctor reference |
| `appointment_id` | BIGINT | FK (appointments.id) | Associated appointment |
| `record_type` | VARCHAR(50) | NOT NULL | Type (CONSULTATION, LAB_RESULT, PRESCRIPTION, IMAGING) |
| `diagnosis` | TEXT | ENCRYPTED | Diagnosis (PHI, encrypted) |
| `treatment` | TEXT | ENCRYPTED | Treatment plan (PHI, encrypted) |
| `notes` | TEXT | ENCRYPTED | Doctor's notes (PHI, encrypted) |
| `file_urls` | TEXT[] | | Array of MinIO file URLs (images, PDFs) |
| `record_date` | TIMESTAMP | NOT NULL | Date of record |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update timestamp |

**Indexes:**
- `idx_medical_records_patient_id` (patient_id)
- `idx_medical_records_doctor_id` (doctor_id)
- `idx_medical_records_record_date` (record_date)
- `idx_medical_records_record_type` (record_type)

**Security:**
- **Field-Level Encryption**: `diagnosis`, `treatment`, `notes` encrypted with AES-256.
- **Audit Logging**: All access logged via `phi_access_logs` table.
- **RBAC**: Patients can only view their own records; doctors can view records for patients they've treated.

---

### 4. Prescriptions Table (`prescriptions`)

**Purpose:** Digital prescriptions with QR codes, drug interaction checks, and e-signature support.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique prescription identifier |
| `patient_id` | BIGINT | NOT NULL, FK (users.id) | Patient reference |
| `doctor_id` | BIGINT | NOT NULL, FK (users.id) | Doctor reference |
| `appointment_id` | BIGINT | FK (appointments.id) | Associated appointment |
| `instructions` | TEXT | | General instructions (e.g., "Take with food") |
| `valid_until` | TIMESTAMP | NOT NULL | Prescription expiration date |
| `qr_code` | TEXT | | Base64-encoded QR code for pharmacy verification |
| `e_sign_status` | VARCHAR(50) | DEFAULT 'PENDING' | E-signature status (PENDING, SIGNED, REJECTED) |
| `e_sign_data` | TEXT | | Doctor's digital signature data |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update timestamp |

**Indexes:**
- `idx_prescriptions_patient_id` (patient_id)
- `idx_prescriptions_doctor_id` (doctor_id)
- `idx_prescriptions_valid_until` (valid_until)

**Child Tables:**
- **`prescription_medications`**: One-to-Many (medications in prescription)
- **`prescription_warnings`**: One-to-Many (drug interactions, allergy warnings)

---

### 5. Prescription Medications Table (`prescription_medications`)

**Purpose:** Individual medications within a prescription.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique medication identifier |
| `prescription_id` | BIGINT | NOT NULL, FK (prescriptions.id) | Parent prescription |
| `name` | VARCHAR(255) | NOT NULL | Drug name |
| `dosage` | VARCHAR(100) | NOT NULL | Dosage (e.g., "500mg") |
| `frequency` | VARCHAR(100) | NOT NULL | Frequency (e.g., "Every 6 hours") |
| `duration` | VARCHAR(100) | NOT NULL | Duration (e.g., "5 days") |
| `instructions` | TEXT | | Specific instructions for this medication |

**Drug Interaction Checks:**
- Automatically checks OpenFDA API for drug interactions.
- Warnings stored in `prescription_warnings` table.

---

### 6. Prescription Warnings Table (`prescription_warnings`)

**Purpose:** Stores drug interaction warnings and allergy alerts.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique warning identifier |
| `prescription_id` | BIGINT | NOT NULL, FK (prescriptions.id) | Parent prescription |
| `warning_type` | VARCHAR(50) | NOT NULL | Type (INTERACTION, ALLERGY, CONTRAINDICATION) |
| `severity` | VARCHAR(20) | NOT NULL | Severity (LOW, MEDIUM, HIGH, CRITICAL) |
| `message` | TEXT | NOT NULL | Warning message |
| `drug_a` | VARCHAR(255) | | First drug in interaction |
| `drug_b` | VARCHAR(255) | | Second drug in interaction |

---

### 7. Payments Table (`payments`)

**Purpose:** Payment tracking with JazzCash integration, dispute handling, and wallet balance.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique payment identifier |
| `appointment_id` | BIGINT | NOT NULL, FK (appointments.id) | Associated appointment |
| `payer_id` | BIGINT | NOT NULL, FK (users.id) | User making payment (usually patient) |
| `payee_id` | BIGINT | NOT NULL, FK (users.id) | User receiving payment (usually doctor) |
| `amount` | DECIMAL(10,2) | NOT NULL | Payment amount (PKR) |
| `currency` | VARCHAR(10) | DEFAULT 'PKR' | Currency code |
| `payment_method` | VARCHAR(50) | NOT NULL | Method (CREDIT_CARD, JAZZCASH, WALLET) |
| `transaction_id` | VARCHAR(255) | UNIQUE | External transaction ID (JazzCash) |
| `status` | VARCHAR(50) | NOT NULL | Status (PENDING, COMPLETED, FAILED, REFUNDED, DISPUTED) |
| `receipt_image_url` | VARCHAR(500) | | MinIO URL for uploaded receipt |
| `failure_reason` | TEXT | | Reason for payment failure |
| `refund_amount` | DECIMAL(10,2) | | Refunded amount (if applicable) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update timestamp |

**Indexes:**
- `idx_payments_payer_id` (payer_id)
- `idx_payments_payee_id` (payee_id)
- `idx_payments_appointment_id` (appointment_id)
- `idx_payments_status` (status)
- `idx_payments_transaction_id` (transaction_id)

**Child Tables:**
- **`payment_disputes`**: One-to-One (dispute details)
- **`payment_dispute_history`**: One-to-Many (dispute status changes)

---

### 8. Payment Disputes Table (`payment_disputes`)

**Purpose:** Manages payment disputes with status tracking and evidence submission.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique dispute identifier |
| `payment_id` | BIGINT | NOT NULL, UNIQUE, FK (payments.id) | Associated payment |
| `raised_by` | BIGINT | NOT NULL, FK (users.id) | User who raised dispute |
| `reason` | TEXT | NOT NULL | Dispute reason |
| `evidence_urls` | TEXT[] | | Array of evidence file URLs (MinIO) |
| `status` | VARCHAR(50) | NOT NULL | Status (PENDING, INVESTIGATING, RESOLVED, REJECTED) |
| `resolution` | TEXT | | Admin's resolution notes |
| `resolved_by` | BIGINT | FK (users.id) | Admin who resolved dispute |
| `resolved_at` | TIMESTAMP | | Resolution timestamp |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Creation timestamp |

**Indexes:**
- `idx_payment_disputes_payment_id` (payment_id)
- `idx_payment_disputes_status` (status)

---

### 9. Facilities Table (`facilities`)

**Purpose:** Healthcare facilities (hospitals, clinics) with location and operating hours.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique facility identifier |
| `name` | VARCHAR(255) | NOT NULL | Facility name |
| `organization_id` | BIGINT | FK (users.id) | Owning organization |
| `facility_type` | VARCHAR(50) | NOT NULL | Type (HOSPITAL, CLINIC, DIAGNOSTIC_CENTER) |
| `address` | TEXT | NOT NULL | Physical address |
| `city` | VARCHAR(100) | NOT NULL | City |
| `province` | VARCHAR(100) | NOT NULL | Province |
| `phone` | VARCHAR(50) | | Contact phone |
| `email` | VARCHAR(255) | | Contact email |
| `latitude` | DECIMAL(10,8) | | GPS latitude |
| `longitude` | DECIMAL(11,8) | | GPS longitude |
| `operating_hours` | JSONB | | Operating hours (JSON: {"monday": "09:00-17:00"}) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Creation timestamp |

**Indexes:**
- `idx_facilities_city` (city)
- `idx_facilities_organization_id` (organization_id)
- `idx_facilities_location` (latitude, longitude) — for proximity search

---

### 10. Video Calls Table (`video_calls`)

**Purpose:** WebRTC video call sessions with Janus Gateway integration.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique call identifier |
| `appointment_id` | BIGINT | NOT NULL, UNIQUE, FK (appointments.id) | Associated appointment |
| `room_id` | VARCHAR(255) | NOT NULL, UNIQUE | Janus room ID |
| `started_at` | TIMESTAMP | | Call start timestamp |
| `ended_at` | TIMESTAMP | | Call end timestamp |
| `duration_seconds` | INT | | Call duration |
| `participants` | JSONB | | Participant details (JSON array) |
| `quality_metrics` | JSONB | | Network quality metrics (JSON) |
| `recording_url` | VARCHAR(500) | | Recording URL (if enabled) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Creation timestamp |

**Indexes:**
- `idx_video_calls_appointment_id` (appointment_id)
- `idx_video_calls_room_id` (room_id)

---

### 11. Notifications Table (`notifications`)

**Purpose:** Real-time notifications for appointments, payments, prescriptions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique notification identifier |
| `user_id` | BIGINT | NOT NULL, FK (users.id) | Recipient user |
| `type` | VARCHAR(50) | NOT NULL | Type (APPOINTMENT_REMINDER, PAYMENT_RECEIVED, PRESCRIPTION_READY) |
| `title` | VARCHAR(255) | NOT NULL | Notification title |
| `message` | TEXT | NOT NULL | Notification message |
| `action_url` | VARCHAR(500) | | Deep link URL for mobile app |
| `is_read` | BOOLEAN | DEFAULT FALSE | Read status |
| `read_at` | TIMESTAMP | | Read timestamp |
| `delivery_status` | VARCHAR(50) | DEFAULT 'PENDING' | Delivery status (PENDING, SENT, FAILED) |
| `delivery_channels` | VARCHAR(100)[] | | Channels (PUSH, EMAIL, SMS) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Creation timestamp |

**Indexes:**
- `idx_notifications_user_id` (user_id)
- `idx_notifications_is_read` (is_read)
- `idx_notifications_created_at` (created_at)

---

### 12. Family Medical History Table (`family_medical_history`)

**Purpose:** Patient's family medical history with genetic conditions and tree visualization.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique history identifier |
| `patient_id` | BIGINT | NOT NULL, FK (users.id) | Patient reference |
| `relation` | VARCHAR(50) | NOT NULL | Relation (PARENT, SIBLING, GRANDPARENT, CHILD) |
| `relation_name` | VARCHAR(255) | | Name of family member |
| `age` | INT | | Age of family member |
| `is_deceased` | BOOLEAN | DEFAULT FALSE | Deceased status |
| `conditions` | TEXT[] | | Array of medical conditions |
| `chronic_diseases` | TEXT[] | | Array of chronic diseases |
| `genetic_conditions` | TEXT[] | | Array of genetic conditions |
| `notes` | TEXT | | Additional notes |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Creation timestamp |

**Indexes:**
- `idx_family_medical_history_patient_id` (patient_id)

---

### 13. Audit Tables

#### PHI Access Logs (`phi_access_logs`)

**Purpose:** Audit trail for all PHI access (HIPAA-like compliance).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique log identifier |
| `actor_id` | BIGINT | NOT NULL, FK (users.id) | User accessing PHI |
| `action` | VARCHAR(100) | NOT NULL | Action (VIEW, UPDATE, DELETE, EXPORT) |
| `resource_type` | VARCHAR(100) | NOT NULL | Resource type (MEDICAL_RECORD, PRESCRIPTION, PATIENT_PROFILE) |
| `resource_id` | BIGINT | NOT NULL | Resource identifier |
| `reason` | TEXT | | Reason for access |
| `ip_address` | VARCHAR(50) | | IP address of request |
| `user_agent` | TEXT | | User agent string |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Access timestamp |

**Indexes:**
- `idx_phi_access_logs_actor_id` (actor_id)
- `idx_phi_access_logs_resource` (resource_type, resource_id)
- `idx_phi_access_logs_created_at` (created_at)

---

## Encryption Strategy

### Field-Level Encryption (AES-256)

**Encrypted Fields:**
- `users.phone`
- `users.cnic`
- `users.allergies` (Patient)
- `users.emergency_contact` (Patient)
- `medical_records.diagnosis`
- `medical_records.treatment`
- `medical_records.notes`

**Implementation:**
- Uses `@Convert` annotation with `PhiEncryptor` converter (javax.persistence.AttributeConverter).
- Encryption key stored as base64-encoded 32-byte key in environment variable `PHI_ENCRYPTION_KEY`.
- **NEVER log or expose encrypted fields in plaintext.**

### Hashing (BCrypt)

- **Password Hashing**: `users.password_hash` uses BCrypt with strength 12.
- **JWT Secret**: HMAC-SHA256 with 256-bit secret key.

---

## Indexing Strategy

### Primary Indexes
- All primary keys (`id`) are clustered B-tree indexes.

### Foreign Key Indexes
- All foreign key columns have non-clustered indexes for join performance.

### Composite Indexes
- **Doctor Overlap Detection**: `(doctor_id, scheduled_at)` on `appointments` table.
- **Payment Queries**: `(payer_id, status)` on `payments` table.
- **Notification Queries**: `(user_id, is_read, created_at)` on `notifications` table.

### Full-Text Search
- Elasticsearch integration for `medical_records`, `prescriptions`, `facilities`.
- Synchronized via Hibernate Search / Spring Data Elasticsearch.

---

## Soft Deletes

**Tables with Soft Delete:**
- `users` (`deleted_at`)
- `appointments` (status change to CANCELLED)
- `medical_records` (optional, based on retention policy)

**Queries:**
- Always filter `WHERE deleted_at IS NULL` to exclude soft-deleted records.
- Admin panel provides "restore" functionality for soft-deleted users.

---

## Data Retention Policy

**Compliance:**
- **Medical Records**: Retain for 7 years (HIPAA requirement).
- **Audit Logs**: Retain for 5 years.
- **Payment Records**: Retain for 10 years (tax compliance).
- **Soft-Deleted Users**: Hard delete after 90 days (GDPR right to erasure).

---

## Performance Considerations

### Connection Pooling
- **HikariCP** (Spring Boot default): 10 connections, max 20.
- **Timeout**: 30 seconds.
- **Leak Detection**: 60 seconds.

### Query Optimization
- Use `LAZY` loading for collections (avoid N+1 queries).
- Batch inserts/updates where possible (Hibernate batch size: 50).
- Use `@EntityGraph` for optimizing JOIN FETCH queries.

### Partitioning (Future)
- **`audit_events`**: Partition by month (time-series data).
- **`notifications`**: Partition by year (archival strategy).

---

## Migration Strategy

### Liquibase
- All schema changes tracked in `src/main/resources/db/changelog/`.
- Changesets versioned with timestamps (e.g., `V001__initial_schema.sql`).
- **Never modify existing changesets** — create new changesets for changes.

### Rollback
- Liquibase supports rollback via `<rollback>` tags.
- Test migrations in staging before production deployment.

---

## Entity Relationships Summary

```
users (1) ──< (N) appointments (patient_id, doctor_id, staff_id)
users (1) ──< (N) medical_records (patient_id, doctor_id)
users (1) ──< (N) prescriptions (patient_id, doctor_id)
users (1) ──< (N) payments (payer_id, payee_id)
users (1) ──< (N) notifications (user_id)
users (1) ──< (N) family_medical_history (patient_id)

appointments (1) ── (1) video_calls (appointment_id)
appointments (1) ── (1) payments (appointment_id)

prescriptions (1) ──< (N) prescription_medications (prescription_id)
prescriptions (1) ──< (N) prescription_warnings (prescription_id)

payments (1) ── (1) payment_disputes (payment_id)
payment_disputes (1) ──< (N) payment_dispute_history (dispute_id)

facilities (1) ──< (N) appointments (facility_id)
organizations (1) ──< (N) facilities (organization_id)
```

---

## Additional Resources

- **Entity Java Files**: `src/main/java/com/healthlink/domain/*/entity/`
- **Liquibase Changelogs**: `src/main/resources/db/changelog/`
- **Repository Interfaces**: `src/main/java/com/healthlink/domain/*/repository/`

For schema modifications, contact the backend team or create a Liquibase changeset.
