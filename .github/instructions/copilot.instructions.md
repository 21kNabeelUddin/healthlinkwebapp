---
applyTo: '**'
---

# HealthLink Copilot Instructions (Backend + Mobile)
Updated: Nov 23, 2025
Date today: Nov 27, 2025
Project: Healthcare SaaS (Spring Boot 3.5.8 + Java 25 + Flutter)

## 1. Absolute Musts (Security & PHI)
- NEVER log, cache, serialize, or emit PHI (names, emails, diagnoses, prescriptions, appointments, lab results) outside secured API responses.
- Use `SafeLogger` for all security / audit logs. For PHI access events call `SafeLogger.audit()` with actor, action, resource, reason.
- Enforce RBAC consistently: check role before exposing PHI fields. Roles: PATIENT, DOCTOR, STAFF, ORGANIZATION, ADMIN, PLATFORM_OWNER.
- Tokens: Short-lived JWT access (≈15m) + rotating refresh. Forced logout via `tokensRevokedAt` check (see `ForcedLogoutFilterTest`).
- Do not introduce local persistence of PHI in mobile (Hive/SecureStorage only for tokens & non-PHI metadata).

## 2. High-Level Architecture
- Backend (`healthlink_backend`): Hexagonal leaning; Spring Boot app with modules for security (`config/SecurityConfig`, `security/jwt`, `security/rate`), domain services (`domain/**`), infrastructure (Redis, Postgres, RabbitMQ, MinIO, Elasticsearch, Sentry, OpenTelemetry).
- Mobile (`healthlink_mobile`): Clean Architecture: `features/<feature>/{domain,data,presentation}`; Bloc/Cubit state; domain layer pure Dart.
- Cross-cutting concerns: PHI encryption, auditing, rate limiting (Bucket4j + Redis), JWT auth, forced logout, drug interaction checks.

## 3. Backend Key Patterns
- Security chain: `SecurityConfig` adds `GlobalRateLimitFilter` then `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.
- Rate limiting: Bucket4j via `RateLimitConfig.proxyManager()` (Redis required). Tests needing context without Redis must mock this.
- Forced logout: JWT claims vs user `tokensRevokedAt` (filter rejects tokens issued before revocation).
- Database migrations: none, using ddl-auto: update; integration tests that don't spin Postgres should use H2.
- Drug interaction: Primary service in `domain/record/service/DrugInteractionService`; deprecated legacy implementations are annotated accordingly—prefer primary.
- Observability: Micrometer + OTLP + Sentry; keep span/trace IDs but exclude PHI from event metadata.

## 4. Mobile Patterns & Conventions
- Bloc naming: `FeatureBloc`, `FeatureEvent`, `FeatureState` using Equatable.
- Progressive PHI disclosure: list screens show non-PHI summaries; full detail only after explicit navigation.
- Theming & design tokens only, no hardcoded colors or text; localization via `context.tr()` (English + Urdu, RTL aware).

## 5. Development Workflow
Backend:
```powershell
# Build & run
cd healthlink_backend; ./gradlew bootRun
# Unit tests / single test
./gradlew test --tests com.healthlink.security.jwt.ForcedLogoutFilterTest
```
Mobile:
```powershell
cd healthlink_mobile
flutter test
flutter analyze
```

## 6. Testing Strategy
- Write/adjust tests in parallel with changes (TDD preference).
- Backend failing context tests often due to external dependencies (Postgres, Redis). Override with test properties: point datasource to H2, mock Redis when not essential to logic.
- Mobile: Unit (domain), widget (presentation), integration (Bloc+Repo), E2E (flows) + specialized (RBAC, PHI, localization, accessibility).
- Use only MOCK PHI in tests.

## 7. External Services & Configuration
- Postgres & Redis (rate limiting) must be available for full context; if absent, add test overrides.
- Other integrations: RabbitMQ (async messaging), MinIO (object storage), Elasticsearch (search), Sentry (error/performance), OpenTelemetry (tracing), Bucket4j (rate limiting), HikariCP (connections).
- Swagger / OpenAPI via SpringDoc (`/swagger-ui`, `/v3/api-docs`). Keep sensitive models excluded or masked.

## 8. Common Pitfalls (Avoid)
- Duplicate Spring beans causing context failure (ensure legacy configs are profiled or removed).
- Logging PHI or placing PHI in exceptions, metrics, tracing spans.
- Storing PHI locally in mobile or leaking via analytics.

## 9. Introducing Changes (AI Agent Guidance)
1. Identify layer: domain vs infrastructure vs presentation.
2. Add/modify tests first; ensure isolation of external resources if not core to logic.
3. Maintain RBAC & PHI rules; run targeted test (`./gradlew test --tests <Class>`) before full suite.
4. Keep instructions minimal, do not refactor unrelated modules while fixing a targeted failure.
5. For new backend filters/services: register ordering explicitly in `SecurityConfig` and audit sensitive paths.
6. Whatever you add/remove in backend/mobile, ensure its counterpart is consistent (e.g., new PHI field in API response must be handled in mobile domain layer).

## 10. Definition of Done (Condensed)
- All new code tested (no failing tests introduced).
- No PHI leakage (logs/traces/errors/analytics).
- RBAC enforced for new endpoints.
- Style & architecture patterns respected (Clean Architecture mobile, modular services backend).
- Observability added without PHI.
