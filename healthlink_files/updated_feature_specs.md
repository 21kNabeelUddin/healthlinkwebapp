# HealthLink Feature Specification (Updated)

**Baseline Reference:** `feature_specs.md` v1.0 (2025-11-24)

**Delta Date:** 2025-11-25

This addendum consolidates every net-new requirement that was captured during the production readiness audit (`comprehensive_remaining_issues.md`). The goal is to keep the canonical feature specification in sync with the live backlog so that engineering, QA, and product all reference a single source of truth. Each section below either extends or clarifies the matching section in the baseline spec.

---

## 1. Platform & Stack (No Change)
- Backend: Java 21 LTS, Spring Boot 3.5.8, Gradle 9.2.1, PostgreSQL 18.1, Redis 8.4.0, RabbitMQ 4.2.1, Elasticsearch 9.2.1, MinIO, Janus, Coturn (NEW: mandatory), Sentry, OpenTelemetry.
- Mobile: Flutter 3.38 / Dart 3.10 targeting Android & iOS (no web build).
- DevOps: Docker 29.0.2, Kubernetes 1.34.2, GitHub Actions, Playwright E2E harness.

---

## 2. New Cross-Cutting Requirements
1. **Staff Video Call Assignment**
   - Service offerings and facilities now expose `requiresStaffAssignment`.
   - Appointment booking must accept `facilityId` & `serviceOfferingId` to enforce routing rules.
   - Auto-assignment picks an available staff member for the facility, avoiding double-booking.
   - Patient and staff check-ins are tracked separately. Staff-required appointments only move to `IN_PROGRESS` after both check-ins succeed.
   - Video call initiation validates patient + staff readiness and propagates staff metadata to signaling responses.

2. **Payment Receipt Upload Revamp**
   - Mobile flows must support camera/gallery capture, client-side compression, PHI encryption prior to upload, and an upload progress indicator.
   - Backend must treat receipt files as PHI: encrypt at rest, never log raw metadata, and include audit events for each access.

3. **Family Medical Tree**
   - Backend entities for relationships + hereditary conditions, including RBAC checks for who may view or edit family members.
   - Mobile tree visualization, CRUD, privacy controls (per-member and per-condition granularity).

4. **Staff 3-Way Video Calls**
   - Dedicated staff layout, IoT accessory hooks, and role-specific controls in the video UI.
   - Mandatory staff check-in validation prior to remote consultations when the service/facility requires it.

5. **WebRTC Testing & Stability**
   - Manual test matrix (2-device + 3-device + adverse network scenarios) must be executed and logged per release.
   - Automated unit/integration suites for WebRTCPeerManager, signaling, SDP, ICE handling, and failure paths.
   - TURN (Coturn) deployment is now non-optional with production-grade credentials and smoke tests baked into CI.

6. **Backend Test Reliability**
   - SecurityConfigTest (13 cases) and SecurityIntegrationTest (11 cases) must pass consistently; bean duplication issues need to be resolved.
   - Appointment, Authentication, Payment, Prescription integration suites must be green.
   - JwtServiceTest + PasswordResetServiceTest flaky assertions fixed.

7. **Code Quality & Security Additions**
   - HtmlConverter dependency repair, duplicate PushNotificationService cleanup, CSRF validation, session fixation prevention, and XSS encoding.
   - JWT deprecation warnings (5 hits) addressed by upgrading sign/verify helpers.
   - Dead imports/variables removed, magic numbers extracted, complex methods refactored, JavaDoc + API docs expanded.

8. **Backend Feature Backlog (Nice-to-Have but Documented)**
   - OpenFDA drug interaction API, prescription templates, family tree backend (mirrors mobile), PDF export service, webhook emissions (8 events), email notifications, payment account routing, dispute escalation, soft delete for GDPR, PMDC + Pakistan Org # validators, appointment overlap prevention, transaction management upgrades, MinIO integration tests, PHI encryption HMACs.

9. **Performance & Optimization Targets**
   - Resolve four N+1 hotspots, add two indices, optimize caching for two services, tighten algorithms & query plans, and tune DB connection pools.

10. **Mobile Polish Items**
    - Offline mode completion, unified API error handling, theme persistence, RTL hardening, password strength indicator, biometric auth, upload progress, and call quality indicators.

11. **Documentation & Infra Enhancements**
    - API/Swagger parity, README + deployment guides, schema docs, integration guides, mobile setup instructions, Docker health checks, volume optimizations, and Kubernetes resource limits.

---

## 3. Detailed Staff Assignment & Video Call Rules
1. **Data Model Updates**
   - `facilities.requiresStaffAssignment` (default `false`).
   - `service_offerings.durationMinutes` (default `15`) and `service_offerings.requiresStaffAssignment`.
   - `appointments` now capture `facility_id`, `service_offering_id`, and `assigned_staff_id`.
   - `video_calls` persist `assigned_staff_id` + `staff_joined_at` timestamps.

2. **Booking Flow**
   - Patient selects doctor ➜ facility ➜ service offering.
   - If the chosen service OR facility requires staff, backend auto-assigns staff via facility availability + overlap checks.
   - Staff conflicts trigger reassignment during booking and rescheduling.

3. **Check-In Flow**
   - Patient and staff check-ins use distinct endpoints & timestamps.
   - Non-staff appointments transition to `IN_PROGRESS` immediately after patient check-in.
   - Staff-required appointments enter `IN_PROGRESS` only when both patient & assigned staff have checked in.
   - Unauthorized staff (not auto-assigned) cannot check in; doctors may override for emergency scenarios while still logging the actor.

4. **Video Call Eligibility**
   - Doctor can initiate WebRTC only after patient check-in and, when applicable, staff check-in.
   - Signaling payload now surfaces `staffParticipantId`, `staffParticipantRequired`, and `staffJoinedAt` to drive 3-way UI states.
   - Backend guards ensure PHI-safe auditing anytime staff tokens are issued.

---

## 4. Testing Expectations per Slice
1. **Slice 1 (Critical)** – Staff assignment & video calls (✅ current focus).
2. **Slice 2 (Critical Issues)** – Payment receipt upload, family tree, staff video UI, manual WebRTC tests.
3. **Slice 3 (Major Issues)** – TURN deployment, automated WebRTC tests, backend test fixes, lint/deprecation cleanup.
4. **Slice 4 (Remaining)** – Feature backlog, performance, documentation, infra polish.

Every slice must finish with:
- Backend `./gradlew test` & `bootJar` green.
- Mobile `flutter test`, `flutter analyze`, and APK generation succeeding.
- `comprehensive_remaining_issues.md` updated to reflect completion status + notes.

---

## 5. Appendix: Traceability Matrix
| Tracker Section | Feature Spec Anchor |
| --- | --- |
| Staff Video Call Feature (Priority 1) | Section 3 of this addendum |
| Mobile Critical Features (Priority 1) | Section 2 points 2-4 |
| Critical Backend Tests | Section 2 point 6 |
| Infrastructure / TURN | Section 2 point 5 |
| Automated WebRTC Tests | Section 2 point 5 & Slice roadmap |
| Code Quality Backend | Section 2 point 7 |
| Backend Features / Performance / Mobile Polish / Docs | Section 2 points 8-11 |

---

**Usage:** Keep `feature_specs.md` as the evergreen functional spec. Use this `updated_feature_specs.md` as the tracked delta until all audit items graduate into the main spec (or are deprecated). Update this file whenever new items enter or exit the production readiness tracker.
