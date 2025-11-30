-- Initialize PostgreSQL database
-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users Table (Single Table Inheritance for all user types)
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_type VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    role VARCHAR(50) NOT NULL,
    approval_status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    is_email_verified BOOLEAN DEFAULT FALSE NOT NULL,
    profile_picture_url VARCHAR(500),
    preferred_language VARCHAR(10) DEFAULT 'en',
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP,
    -- Patient specific fields
    date_of_birth DATE,
    gender VARCHAR(20),
    blood_group VARCHAR(10),
    address VARCHAR(500),
    city VARCHAR(100),
    country VARCHAR(100) DEFAULT 'Pakistan',
    emergency_contact_name VARCHAR(200),
    emergency_contact_phone VARCHAR(20),
    -- Doctor specific fields
    pmdc_id VARCHAR(20) UNIQUE,
    pmdc_verified BOOLEAN DEFAULT FALSE,
    license_document_url VARCHAR(500),
    specialization VARCHAR(200),
    years_of_experience INTEGER,
    bio VARCHAR(2000),
    consultation_fee DECIMAL(10,2),
    average_rating DOUBLE PRECISION DEFAULT 0.0,
    total_reviews INTEGER DEFAULT 0,
    allow_early_checkin BOOLEAN DEFAULT FALSE,
    early_checkin_minutes INTEGER DEFAULT 15,
    slot_duration_minutes INTEGER DEFAULT 30,
    -- Organization specific fields
    organization_name VARCHAR(300),
    pakistan_org_number VARCHAR(20) UNIQUE,
    org_verified BOOLEAN DEFAULT FALSE,
    registration_document_url VARCHAR(500),
    organization_type VARCHAR(100),
    headquarters_address VARCHAR(500),
    website_url VARCHAR(300),
    total_doctors INTEGER DEFAULT 0,
    total_staff INTEGER DEFAULT 0,
    total_facilities INTEGER DEFAULT 0,
    payment_account_mode VARCHAR(50) DEFAULT 'DOCTOR_LEVEL',
    -- Staff specific fields
    added_by_doctor_id UUID,
    added_by_org_id UUID,
    assigned_facility_id UUID,
    can_manage_appointments BOOLEAN DEFAULT TRUE,
    can_record_payments BOOLEAN DEFAULT TRUE,
    is_available BOOLEAN DEFAULT TRUE,
    -- Admin specific fields
    admin_username VARCHAR(100) UNIQUE,
    can_approve_doctors BOOLEAN DEFAULT TRUE,
    can_approve_organizations BOOLEAN DEFAULT TRUE,
    can_view_analytics BOOLEAN DEFAULT TRUE,
    created_by_platform_owner_id UUID,
    -- Platform Owner specific fields
    owner_username VARCHAR(100) UNIQUE,
    has_full_access BOOLEAN DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_user_approval_status ON users(approval_status);

-- Facilities Table (must be before appointments due to FK)
CREATE TABLE IF NOT EXISTS facilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES users(id),
    doctor_owner_id UUID REFERENCES users(id),
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    requires_staff_assignment BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_facility_org ON facilities(organization_id);
CREATE INDEX IF NOT EXISTS idx_facility_doctor ON facilities(doctor_owner_id);

-- Service Offerings Table (must be before appointments due to FK)
CREATE TABLE IF NOT EXISTS service_offerings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL REFERENCES facilities(id),
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    base_fee DECIMAL(10,2),
    duration_minutes INTEGER DEFAULT 15 NOT NULL,
    requires_staff_assignment BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_service_facility ON service_offerings(facility_id);

-- Appointments Table
CREATE TABLE IF NOT EXISTS appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES users(id),
    patient_id UUID NOT NULL REFERENCES users(id),
    facility_id UUID REFERENCES facilities(id),
    service_offering_id UUID REFERENCES service_offerings(id),
    assigned_staff_id UUID REFERENCES users(id),
    appointment_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason_for_visit VARCHAR(500),
    notes VARCHAR(2000),
    is_checked_in BOOLEAN DEFAULT FALSE,
    check_in_time TIMESTAMP,
    patient_check_in_time TIMESTAMP,
    staff_check_in_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_appointment_doctor ON appointments(doctor_id);
CREATE INDEX IF NOT EXISTS idx_appointment_patient ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointment_time ON appointments(appointment_time);
CREATE INDEX IF NOT EXISTS idx_appointment_status ON appointments(status);
CREATE INDEX IF NOT EXISTS idx_appointment_facility ON appointments(facility_id);
CREATE INDEX IF NOT EXISTS idx_appointment_staff ON appointments(assigned_staff_id);

-- Payments Table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(id),
    amount DECIMAL(10,2) NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    transaction_reference VARCHAR(255),
    receipt_url VARCHAR(500),
    verified_by_user_id UUID REFERENCES users(id),
    verification_notes VARCHAR(500),
    verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_appointment ON payments(appointment_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payments(status);

-- Video Calls Table
CREATE TABLE IF NOT EXISTS video_calls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(id),
    assigned_staff_id UUID REFERENCES users(id),
    janus_session_id BIGINT,
    janus_handle_id BIGINT,
    room_secret VARCHAR(255),
    recording_url VARCHAR(500),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    staff_joined_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_videocall_appointment ON video_calls(appointment_id);

-- Medical Records Table
CREATE TABLE IF NOT EXISTS medical_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id),
    doctor_id UUID REFERENCES users(id),
    record_type VARCHAR(50) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_hash VARCHAR(255),
    description VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_medrecord_patient ON medical_records(patient_id);
CREATE INDEX IF NOT EXISTS idx_medrecord_doctor ON medical_records(doctor_id);

-- Doctor Qualifications Table (for ElementCollection)
CREATE TABLE IF NOT EXISTS doctor_qualifications (
    doctor_id UUID NOT NULL REFERENCES users(id),
    qualification VARCHAR(500) NOT NULL,
    PRIMARY KEY (doctor_id, qualification)
);

-- Audit Events Table
CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    operation VARCHAR(64) NOT NULL,
    target_ref VARCHAR(128),
    details VARCHAR(512),
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_events(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_operation ON audit_events(operation);
CREATE INDEX IF NOT EXISTS idx_audit_target ON audit_events(target_ref);

-- Refresh Tokens Table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE NOT NULL,
    revoked_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_revoked ON refresh_tokens(revoked);

-- Reviews Table
CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES users(id),
    patient_id UUID NOT NULL REFERENCES users(id),
    appointment_id UUID NOT NULL REFERENCES appointments(id),
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comments VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_review_doctor ON reviews(doctor_id);
CREATE INDEX IF NOT EXISTS idx_review_patient ON reviews(patient_id);
CREATE INDEX IF NOT EXISTS idx_review_appointment ON reviews(appointment_id);

-- Service Schedules Table
CREATE TABLE IF NOT EXISTS service_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_offering_id UUID NOT NULL REFERENCES service_offerings(id),
    day_of_week INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_serviceschedule_offering ON service_schedules(service_offering_id);

-- Payment Accounts Table
CREATE TABLE IF NOT EXISTS payment_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID REFERENCES users(id),
    organization_id UUID REFERENCES users(id),
    account_details VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payacct_doctor ON payment_accounts(doctor_id);
CREATE INDEX IF NOT EXISTS idx_payacct_org ON payment_accounts(organization_id);

-- Payment Verifications Table
CREATE TABLE IF NOT EXISTS payment_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(id),
    verifier_user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(512),
    verified_at TIMESTAMP,
    disputed BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payverify_payment ON payment_verifications(payment_id);
CREATE INDEX IF NOT EXISTS idx_payverify_status ON payment_verifications(status);

-- Payment Disputes Table
CREATE TABLE IF NOT EXISTS payment_disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    verification_id UUID NOT NULL REFERENCES payment_verifications(id),
    stage VARCHAR(50) NOT NULL,
    resolution_status VARCHAR(50) DEFAULT 'OPEN' NOT NULL,
    raised_by_user_id UUID NOT NULL REFERENCES users(id),
    notes VARCHAR(1000),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dispute_verification ON payment_disputes(verification_id);
CREATE INDEX IF NOT EXISTS idx_dispute_stage ON payment_disputes(stage);

-- Prescriptions Table
CREATE TABLE IF NOT EXISTS prescriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id),
    doctor_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

-- Lab Orders Table
CREATE TABLE IF NOT EXISTS lab_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id),
    order_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    ordered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    result_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

-- Family Medical Tree Table
CREATE TABLE IF NOT EXISTS family_medical_tree (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id),
    relative_name VARCHAR(200) NOT NULL,
    relationship VARCHAR(100) NOT NULL,
    condition VARCHAR(300) NOT NULL,
    diagnosed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_famtree_patient ON family_medical_tree(patient_id);

-- Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    message VARCHAR(500) NOT NULL,
    scheduled_at TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_scheduled ON notifications(scheduled_at);

-- Notification Preferences Table
CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    appointment_reminder_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    payment_status_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    cancellation_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    reminder_offsets VARCHAR(100) DEFAULT '60,15,5',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifpref_user ON notification_preferences(user_id);

-- Consent Versions Table
CREATE TABLE IF NOT EXISTS consent_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consent_version VARCHAR(50) NOT NULL UNIQUE,
    language VARCHAR(10) NOT NULL,
    content VARCHAR(8000) NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

-- User Consents Table
CREATE TABLE IF NOT EXISTS user_consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    consent_version VARCHAR(50) NOT NULL,
    accepted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_userconsent_user ON user_consents(user_id);
CREATE INDEX IF NOT EXISTS idx_userconsent_version ON user_consents(consent_version);

-- Webhook Subscriptions Table
CREATE TABLE IF NOT EXISTS webhook_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(50) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_webhook_owner ON webhook_subscriptions(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_webhook_event ON webhook_subscriptions(event_type);

-- Published Events Table
CREATE TABLE IF NOT EXISTS published_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(100),
    target_url VARCHAR(500),
    published_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    delivery_status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
    retry_count INTEGER DEFAULT 0,
    last_error_message VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pubevent_status ON published_events(delivery_status);
CREATE INDEX IF NOT EXISTS idx_pubevent_published ON published_events(published_at);

-- ============================================
-- SEED DATA FOR DEVELOPMENT/TESTING
-- ============================================

-- Platform Owner (password: PlatformOwner@123)
INSERT INTO users (
    id, user_type, email, password_hash, first_name, last_name, phone_number,
    role, approval_status, is_active, is_email_verified,
    owner_username, has_full_access, created_at
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'PLATFORM_OWNER',
    'owner@healthlink.pk',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQDu1Nk/DqnPfhbVKBPPzyNF1IWWGJq',
    'Platform', 'Owner', '+923001234567',
    'PLATFORM_OWNER', 'APPROVED', TRUE, TRUE,
    'platformowner', TRUE, CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

-- Admin (password: Admin@123)
INSERT INTO users (
    id, user_type, email, password_hash, first_name, last_name, phone_number,
    role, approval_status, is_active, is_email_verified,
    admin_username, can_approve_doctors, can_approve_organizations, can_view_analytics,
    created_by_platform_owner_id, created_at
) VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'ADMIN',
    'admin@healthlink.pk',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQDu1Nk/DqnPfhbVKBPPzyNF1IWWGJq',
    'System', 'Admin', '+923001234568',
    'ADMIN', 'APPROVED', TRUE, TRUE,
    'admin', TRUE, TRUE, TRUE,
    'a0000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

-- Patient (password: Patient@123)
INSERT INTO users (
    id, user_type, email, password_hash, first_name, last_name, phone_number,
    role, approval_status, is_active, is_email_verified,
    date_of_birth, gender, blood_group, address, city, country,
    emergency_contact_name, emergency_contact_phone, created_at
) VALUES (
    'a0000000-0000-0000-0000-000000000003',
    'PATIENT',
    'patient@test.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQDu1Nk/DqnPfhbVKBPPzyNF1IWWGJq',
    'Test', 'Patient', '+923001234569',
    'PATIENT', 'APPROVED', TRUE, TRUE,
    '1990-05-15', 'MALE', 'O+', '123 Test Street', 'Karachi', 'Pakistan',
    'Emergency Contact', '+923001234570', CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

-- Doctor (password: Doctor@123)
INSERT INTO users (
    id, user_type, email, password_hash, first_name, last_name, phone_number,
    role, approval_status, is_active, is_email_verified,
    pmdc_id, pmdc_verified, specialization, years_of_experience, bio,
    consultation_fee, average_rating, total_reviews,
    slot_duration_minutes, allow_early_checkin, early_checkin_minutes, created_at
) VALUES (
    'a0000000-0000-0000-0000-000000000004',
    'DOCTOR',
    'doctor@test.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQDu1Nk/DqnPfhbVKBPPzyNF1IWWGJq',
    'Sarah', 'Smith', '+923001234571',
    'DOCTOR', 'APPROVED', TRUE, TRUE,
    'PMDC-12345', TRUE, 'General Medicine', 10,
    'Experienced general medicine doctor specializing in preventive care.',
    2000.00, 4.5, 50,
    30, TRUE, 15, CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

-- Doctor Qualifications
INSERT INTO doctor_qualifications (doctor_id, qualification) VALUES
    ('a0000000-0000-0000-0000-000000000004', 'MBBS - Aga Khan University'),
    ('a0000000-0000-0000-0000-000000000004', 'FCPS - General Medicine')
ON CONFLICT (doctor_id, qualification) DO NOTHING;

-- Organization (password: Organization@123)
INSERT INTO users (
    id, user_type, email, password_hash, first_name, last_name, phone_number,
    role, approval_status, is_active, is_email_verified,
    organization_name, pakistan_org_number, org_verified, organization_type,
    headquarters_address, website_url, total_doctors, total_staff, total_facilities,
    payment_account_mode, created_at
) VALUES (
    'a0000000-0000-0000-0000-000000000005',
    'ORGANIZATION',
    'org@healthlink.pk',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQDu1Nk/DqnPfhbVKBPPzyNF1IWWGJq',
    'HealthCare', 'Organization', '+923001234572',
    'ORGANIZATION', 'APPROVED', TRUE, TRUE,
    'HealthCare Plus Hospital', 'PKO-001234', TRUE, 'HOSPITAL',
    '456 Hospital Road, Karachi', 'https://healthcareplus.pk', 25, 50, 3,
    'ORGANIZATION_LEVEL', CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

-- Staff member added by doctor (password: Staff@123)
INSERT INTO users (
    id, user_type, email, password_hash, first_name, last_name, phone_number,
    role, approval_status, is_active, is_email_verified,
    added_by_doctor_id, can_manage_appointments, can_record_payments, is_available, created_at
) VALUES (
    'a0000000-0000-0000-0000-000000000006',
    'STAFF',
    'staff@test.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQDu1Nk/DqnPfhbVKBPPzyNF1IWWGJq',
    'Test', 'Staff', '+923001234573',
    'STAFF', 'APPROVED', TRUE, TRUE,
    'a0000000-0000-0000-0000-000000000004', TRUE, TRUE, TRUE, CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

-- Staff member added by organization (password: OrgStaff@123)
INSERT INTO users (
    id, user_type, email, password_hash, first_name, last_name, phone_number,
    role, approval_status, is_active, is_email_verified,
    added_by_org_id, can_manage_appointments, can_record_payments, is_available, created_at
) VALUES (
    'a0000000-0000-0000-0000-000000000007',
    'STAFF',
    'orgstaff@healthlink.pk',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQDu1Nk/DqnPfhbVKBPPzyNF1IWWGJq',
    'Organization', 'Staff', '+923001234574',
    'STAFF', 'APPROVED', TRUE, TRUE,
    'a0000000-0000-0000-0000-000000000005', TRUE, TRUE, TRUE, CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

-- Facility for the doctor
INSERT INTO facilities (
    id, doctor_owner_id, name, address, active, requires_staff_assignment
) VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000004',
    'Dr. Sarah Smith Clinic',
    '789 Medical Center, Karachi',
    TRUE, FALSE
) ON CONFLICT (id) DO NOTHING;

-- Facility for the organization
INSERT INTO facilities (
    id, organization_id, name, address, active, requires_staff_assignment
) VALUES (
    'b0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000005',
    'HealthCare Plus Main Hospital',
    '456 Hospital Road, Karachi',
    TRUE, TRUE
) ON CONFLICT (id) DO NOTHING;

-- Service offerings for doctor's facility
INSERT INTO service_offerings (
    id, facility_id, name, description, base_fee, duration_minutes, requires_staff_assignment
) VALUES 
    ('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001',
     'General Consultation', 'Standard consultation with the doctor', 2000.00, 30, FALSE),
    ('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001',
     'Follow-up Visit', 'Follow-up appointment for existing patients', 1500.00, 15, FALSE)
ON CONFLICT (id) DO NOTHING;

-- Service offerings for organization facility
INSERT INTO service_offerings (
    id, facility_id, name, description, base_fee, duration_minutes, requires_staff_assignment
) VALUES 
    ('c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002',
     'Emergency Care', 'Emergency medical care', 5000.00, 60, TRUE),
    ('c0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000002',
     'General Checkup', 'Complete health checkup package', 10000.00, 120, TRUE)
ON CONFLICT (id) DO NOTHING;

-- Update staff with assigned facility
UPDATE users SET assigned_facility_id = 'b0000000-0000-0000-0000-000000000001'
WHERE id = 'a0000000-0000-0000-0000-000000000006';

UPDATE users SET assigned_facility_id = 'b0000000-0000-0000-0000-000000000002'
WHERE id = 'a0000000-0000-0000-0000-000000000007';

-- Print seed summary
DO $$
BEGIN
    RAISE NOTICE '=== HEALTHLINK SEED DATA SUMMARY ===';
    RAISE NOTICE 'Platform Owner: owner@healthlink.pk';
    RAISE NOTICE 'Admin: admin@healthlink.pk';
    RAISE NOTICE 'Patient: patient@test.com';
    RAISE NOTICE 'Doctor: doctor@test.com (Dr. Sarah Smith)';
    RAISE NOTICE 'Organization: org@healthlink.pk (HealthCare Plus Hospital)';
    RAISE NOTICE 'Staff (doctor): staff@test.com';
    RAISE NOTICE 'Staff (org): orgstaff@healthlink.pk';
    RAISE NOTICE 'All passwords: Use bcrypt hash for respective role (e.g., Doctor@123)';
    RAISE NOTICE '====================================';
END $$;
