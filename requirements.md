# HealthLink Requirements Specification

## 1. User Roles & Frontend Access Matrix

**Table 1: HealthLink: User Roles and Access Matrix**

| Role | Mobile App | Web Interface | Account Source | Auth Method |
| :--- | :--- | :--- | :--- | :--- |
| **Patient** | Yes | Yes | Self-Signup | Email + Password + OTP |
| **Doctor** | Yes | Yes | Self-Signup | Email + Password + OTP |
| **Staff** | Yes | Yes | Invite (by Doc/Inst) | Email + Password + OTP |
| **Institution** | No | Yes | Self-Signup | Email + Password + OTP |
| **Admin** | No | Yes | Invite (by Owner) | Email + Password + OTP |
| **Owner** | No | Yes | System Seed | Email + Password + OTP |

---

## 2. Functional Requirements

**Table 2: Functional Requirements Matrix**

| Module | Feature | User Roles | Critical Logic & Constraints |
| :--- | :--- | :--- | :--- |
| **IAM** | Auth & Session Mgmt | All | 15min Access Token / 7d Refresh. Forced logout on 'Pending' status. |
| **IAM** | Staff Management | Doc, Inst | Account created by Doc/Inst. Doctor/Inst can toggle staff availability; staff cannot. |
| **Facility** | Practice Management | Doc, Inst | Practice → Facility → Service → Schedule. Multiple clinics per doctor. Hierarchical assignment. |
| **Appointment** | Booking | Patient | Book Slot → Pay → Confirm. |
| **Appointment** | Emergency | Doc, Staff | Emergency Patient registration flow. Bypasses payment verification for instant record creation; flagged URGENT. |
| **Telehealth** | 3-Party Video | Doc, Pat, Staff | Doctor, Patient, and optionally Staff. External camera/mic toggle on both Web and Mobile for Staff. Self-hosted Janus WebRTC Gateway. |
| **Clinical** | EHR & Records | Doc, Staff | Secure, encrypted PHI. Recursive family medical tree with privacy controls. |
| **Clinical** | Storage | All | SeaweedFS (Open source S3 alternative), encrypted at rest. |
| **Billing** | Manual Payment | Patient, Staff, Doc, Inst | Receipt Upload (encrypted as PHI), Verified by Staff/Doc/Inst. Dispute flow escalates: Staff → Doc → Owner → Admin → Court. |
| **Search** | Doctor Discovery | Patient | Elasticsearch: specialty, location, rating, emergency filter. Fast faceted search. |
| **Analytics** | Dashboards | Doc, Inst, Admin | Revenue, rating statistics, anonymized system metrics. No PHI for Admin/Owner. |
| **Notifications** | In-app/Push | All | No email/SMS notifications. Configurable reminders (default: 1h, 15m, 5m). |
| **Legal & Consent** | Digital Consent | Patient | Required before remote appointment booking. Versioned, English and Urdu. |

---

## 3. Technical Architecture Specifications

### Backend (Spring Boot + Spring Modulith)
* **Database**: PostgreSQL 18.1, single schema (`public`), table prefixes per module (e.g., `iam_users`, `fac_practices`).
* **Object Storage**: SeaweedFS (open-source S3-compatible), at-rest encryption for PHI.
* **Cache**: Redis (session & OTP management).
* **Search**: Elasticsearch (for doctor, facility, service search).
* **API Documentation**: OpenAPI 3.0 (Swagger UI).

### Mobile Frontend (Flutter)
* **Architecture**: Feature-First Clean Architecture, BLoC.
* **Language**: English only.
* **Video**: WebRTC via Janus Gateway integration (`flutter_webrtc`).
* **Networking**: RESTful API via Dio or Chopper.

### Web Frontend (React + TypeScript)
* **Dashboards**: Six separate role-based interfaces (Patient, Doctor, Staff, Institution, Admin, Owner).
* **Video**: WebRTC integration for doctor, patient, staff (camera/mic source switching via browser API).
* **Performance**: Code splitting (`React.lazy`), fast search, responsive layouts, analytics & approval UIs for Admin/Owner.

---

## 4. Non-Functional Requirements

### Compliance
* HIPAA-grade at-rest (AES-256)/in-transit (TLS 1.3) PHI encryption.
* Complete audit logs for all PHI accesses (read/write/delete).
* Role-based anonymization for system analytics.

### Performance
* **Backend**: Virtual threads (Java 21), DTO projections, Redis-backed rate limits.
* **Mobile**: Use Flutter slivers/lazy lists for large datasets, compress user file uploads client-side.
* **Web**: Code splitting, optimized search queries, client-side caching for dashboard stats.

### Reliability
* Circuit breakers on Telehealth endpoints (Resilience4j).
* Graceful fallback from video to audio in Telehealth.
* Event-based escalation chain for payment disputes and emergencies.

---

## 5. Emergency Patient Workflow (Feature Addition)

**Initiation**: Staff or Doctor can create "Emergency Patient" profile flagged as URGENT.

**Flow**:
1. Registration bypasses payment verification.
2. Staff or Doctor completes rapid medical record entry.
3. Appointment/treatment flagged for priority in analytics and notifications.
4. Follow-up can convert emergency patient to regular patient by filling standard registration info later.

**Auditing**: All emergency accesses and modifications are logged for compliance.

---

## 6. Further Recommendations

* **Object Storage**: SeaweedFS.
* Keep RBAC policy and permissions management at the code level as well as database triggers for critical workflows (payment verification, emergency workflows).
* Use OpenAPI contract-first for rapid frontend-backend iteration and strong type safety in all layers.

---

## References

1. **SeaweedFS Documentation**: https://github.com/seaweedfs/seaweedfs
2. **Spring Modulith Reference**: https://spring.io/projects/spring-modulith
3. **Spring Boot Best Practices**: https://spring.io/guides
4. **Flutter Clean Architecture Guide**: https://docs.flutter.dev/app-architecture/guide
5. **Elasticsearch Official Docs**: https://www.elastic.co/guide/
6. **OpenAPI Spec**: https://swagger.io/specification/
7. **WebRTC for Healthcare**: https://webrtc.org/
