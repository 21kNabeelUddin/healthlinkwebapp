# Recent Changes — Admin Portal

## What’s done
- User/doctor status display fixed (verified/approval/active) and doctor detail view (profile, clinics, appointments) wired on Admin → Doctors.
- Admin appointments page: conflict detection tightened (same doctor, same day, <30 mins), full month grid, reschedule modal (date+time), drag/drop + list reschedule persist via backend, reminders wired to email endpoint, cancelled appointments filtered out, delete action visible.
- Doctor signup: specialization and years of experience now dropdowns; PMDC ID surfaces in admin and doctor profile views.
- Doctor profile (doctor portal): pulls `/users/me`, normalizes fields, shows actual clinic counts via facilities API.
- Admin clinics page: consistent sidebar, detail modal fixed, delete button added; backend delete now cancels related appointments, soft-deletes clinic, notifies doctor (in-app + email).

## What’s still missing or broken (and how to fix)
- Clinic delete 500: frontend is sending a non-UUID ID; use the facility’s UUID from the API when calling `DELETE /api/v1/facilities/{id}`. Optionally, harden backend to return 400 on invalid UUID strings.
- Admin appointment delete 403: ensure backend restarted with latest `AppointmentController` (ADMIN allowed) and admin token has ROLE_ADMIN. Backend DB connectivity must be working or deletes will fail.
- Optional services not configured: Redis (blacklist warnings) and OTEL exporter to `localhost:4318` will fail on Render/Vercel. Either provide those services or disable Redis blacklist/OTEL exporter in prod config.
- Appointment reminders/templates: email reminder endpoint exists; still need production template/content and scheduling config if required.
- PMDC verification UI: not built; add an admin/staff page to review/approve/revoke PMDC licenses and view documents.
- Responsiveness/accessibility: no recent pass for mobile/WCAG; plan a responsive QA sweep and basic a11y fixes.

## Notes for deployment (Render/Vercel)
- Verify environment: DB host reachable, Redis optional toggled off if absent, OTEL exporter disabled or pointed to a collector.
- Restart backend after pulling latest changes so clinic delete + admin cancel are active.

