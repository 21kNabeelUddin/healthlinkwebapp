# Recent Changes - Post Git Pull

This document tracks all changes made to the codebase after running `git pull origin main`.

## Date: 2025-01-06

---

## 🔄 Major Changes After Git Pull

### 1. **Doctor Search & Booking Flow Redesign**

#### Problem
- Patient portal showed "0 doctors found" despite doctors being set up
- Booking flow was confusing - patients had to select doctor first, then check if clinic exists
- No way to see all clinics for a doctor before booking

#### Solution Implemented

**A. Enhanced Doctor Search Page (`/patient/doctors`)**
- ✅ Created comprehensive doctor search page with:
  - Real-time search by name, specialty, or location
  - Advanced filters (Specialty, Location, Rating, Fee Range, Consultation Type)
  - Grid and List view modes
  - Sorting options (Rating, Fee, Experience, Name)
  - Beautiful doctor cards with all information
- ✅ Changed "Book Appointment" button to "View Profile" on doctor cards
- ✅ Location-based filtering - only shows doctors who have clinics in selected city
- ✅ Doctor cards now display ALL clinics with locations

**B. New Doctor Profile Page (`/patient/doctors/[doctorId]`)**
- ✅ Created dedicated doctor profile page
- ✅ Shows all active clinics for the doctor
- ✅ Each clinic card displays:
  - Clinic name and address
  - Operating hours (opens/closes)
  - Consultation fee
  - "Book Appointment" button
- ✅ Patient can see all clinic options before booking

**C. Enhanced Booking Page (`/patient/doctors/[doctorId]/book`)**
- ✅ Pre-selects clinic if `clinicId` is in URL query params
- ✅ Shows selected clinic details:
  - Full address
  - Operating hours (opens/closes times)
  - Consultation fee
- ✅ Clinic selection is required for on-site appointments
- ✅ Shows warning if doctor has no clinics
- ✅ Back button links to doctor profile page

**D. Redesigned Doctor Appointments Page (`/doctor/appointments`)**
- ✅ Appointments now grouped by clinic
- ✅ Each clinic section shows:
  - Clinic name with appointment count badge
  - Clinic address and operating hours
  - All appointments for that clinic
- ✅ "Online Consultations" section for online appointments
- ✅ Dual filters:
  - Status filter (Pending, Confirmed, etc.)
  - Clinic filter (All Clinics, Online, or specific clinic)
- ✅ Appointment details include clinic information

### 2. **Fixed Doctor Listing Issue**

#### Problem
- Patient portal showed "0 doctors found" even though doctors existed
- Backend was filtering by `pmdcVerified = true` which was too restrictive

#### Solution
- ✅ Removed `pmdcVerified` requirement from doctor listing queries
- ✅ Doctors now only need: `approvalStatus = 'APPROVED'`, `isActive = true`, and not deleted
- ✅ Updated `DoctorRepository` queries:
  - `findAllVerifiedAndApproved()` - removed pmdcVerified check
  - `searchBySpecialization()` - removed pmdcVerified check
  - `findByMinimumRating()` - removed pmdcVerified check
  - `searchDoctors()` - removed pmdcVerified check

### 3. **PMDC Verification System**

#### New Feature
- ✅ Created `DoctorVerificationController` for admin/staff to verify PMDC licenses
- ✅ New endpoints:
  - `GET /api/v1/admin/doctors/verification-status` - View all doctors with verification status
  - `GET /api/v1/admin/doctors/pending-verification` - View doctors pending verification
  - `POST /api/v1/admin/doctors/{doctorId}/verify-pmdc` - Verify a doctor's PMDC license
  - `POST /api/v1/admin/doctors/{doctorId}/revoke-pmdc` - Revoke PMDC verification
- ✅ Accessible by ADMIN and STAFF roles
- ✅ Separate from account approval workflow

### 4. **Fixed Clinic Listing for Patients**

#### Problem
- Patient profile page showed "No clinics available" even when clinics existed
- Endpoint `/api/v1/facilities/doctor/{doctorId}` only allowed DOCTOR and ADMIN roles

#### Solution
- ✅ Updated `FacilitiesController.listDoctor()` to allow PATIENT role
- ✅ Patients can now view clinics for any doctor (for booking purposes)
- ✅ Doctors can still only view their own clinics
- ✅ Admins can view any doctor's clinics

### 5. **Extended JWT Token Expiration**

#### Problem
- Access tokens expired after 15 minutes, causing frequent logouts
- Very annoying user experience

#### Solution
- ✅ Extended access token expiration from 15 minutes to 4 hours
- ✅ Updated `application.yml`: `access-token-expiration: 14400000` (4 hours)
- ✅ Updated `AuthenticationService`: `expiresIn(14400000L)` in response
- ✅ Refresh token remains 7 days (unchanged)

### 6. **Added "Doctors" to Patient Sidebar**

#### Change
- ✅ Added "Doctors" menu item to patient sidebar navigation
- ✅ Direct access to `/patient/doctors` from sidebar
- ✅ Uses Stethoscope icon

### 7. **Updated Doctor Card Design**

#### Changes
- ✅ Removed "View Profile" button (replaced with direct "Book Appointment")
- ✅ Removed languages from details box
- ✅ Removed experience from details box
- ✅ Experience now in separate highlighted section
- ✅ Consultation fee in separate gradient box
- ✅ Shows all clinics for the doctor
- ✅ Matches design mockup specifications

### 8. **Appointment Type Storage Implementation** (Partial - Has Issues)

#### Changes Made
- ✅ Added `type` field to `CreateAppointmentRequest` DTO (ONLINE/ONSITE)
- ✅ Backend stores appointment type in notes field with prefix `APPT_TYPE:ONLINE` or `APPT_TYPE:ONSITE`
- ✅ Frontend sends appointment type when creating appointments
- ✅ Frontend transformation function extracts type from notes field
- ✅ Updated doctor appointments page grouping logic to separate ONLINE and ONSITE

#### Known Issue
- ⚠️ Appointments are still incorrectly grouped (see Known Issues section)
- Backend may need restart for changes to take effect
- Transformation may not be working correctly for all cases

---

## 📁 Files Modified

### Backend
- `healthlink_backend/src/main/java/com/healthlink/domain/user/repository/DoctorRepository.java`
  - Removed `pmdcVerified` requirement from all queries
  
- `healthlink_backend/src/main/java/com/healthlink/domain/user/controller/DoctorVerificationController.java`
  - New controller for PMDC verification management
  
- `healthlink_backend/src/main/java/com/healthlink/domain/organization/controller/FacilitiesController.java`
  - Updated `listDoctor()` to allow PATIENT role access
  
- `healthlink_backend/src/main/resources/application.yml`
  - Extended access token expiration to 4 hours
  
- `healthlink_backend/src/main/java/com/healthlink/service/auth/AuthenticationService.java`
  - Updated `expiresIn` to 4 hours in authentication response

### Frontend
- `frontend/app/patient/doctors/page.tsx`
  - Complete redesign with search, filters, and enhanced doctor cards
  - Changed button to "View Profile"
  - Shows all clinics in doctor cards
  - Location-based filtering
  
- `frontend/app/patient/doctors/[doctorId]/page.tsx`
  - New doctor profile page showing all clinics
  
- `frontend/app/patient/doctors/[doctorId]/book/page.tsx`
  - Enhanced booking page with clinic hours display
  - Pre-selects clinic from URL
  - Shows clinic details when selected
  
- `frontend/app/doctor/appointments/page.tsx`
  - Complete redesign - appointments grouped by clinic
  - Clinic filter added
  - Appointment counts per clinic
  
- `frontend/app/patient/sidebar-items.ts`
  - Added "Doctors" menu item
  
- `frontend/app/patient/appointments/page.tsx`
  - Updated "Book New Appointment" to link to `/patient/doctors`
  - Added date validation to prevent "Invalid time value" errors
  
- `frontend/app/patient/dashboard/page.tsx`
  - Updated "Book Appointment" button to link to `/patient/doctors`
  
- `frontend/app/patient/notifications/page.tsx`
  - Updated "Book again" link to `/patient/doctors`
  
- `frontend/app/patient/appointments/book/page.tsx`
  - Now redirects to new booking flow
  
- `frontend/types/index.ts`
  - Added `averageRating`, `totalReviews`, `consultationFee` to Doctor interface

- `frontend/lib/api.ts`
  - Added `transformAppointment` function to map backend response to frontend format
  - Extracts appointment type from notes field (`APPT_TYPE:ONLINE` or `APPT_TYPE:ONSITE`)
  - Maps `startTime` to `appointmentDateTime`
  - Validates dates before formatting

- `healthlink_backend/src/main/java/com/healthlink/domain/appointment/dto/CreateAppointmentRequest.java`
  - Added `type` field (String) for appointment type (ONLINE/ONSITE)
  - Added `notes` field (String) for additional notes

- `healthlink_backend/src/main/java/com/healthlink/domain/appointment/service/AppointmentService.java`
  - Stores appointment type in notes field with prefix `APPT_TYPE:`
  - Extracts appointment type from notes when mapping to response
  - Sets `type` field in `AppointmentResponse`

---

## 🔧 Configuration Changes

### JWT Token Expiration
- **Before**: 15 minutes (900000ms)
- **After**: 4 hours (14400000ms)
- **Location**: `healthlink_backend/src/main/resources/application.yml`

### API Access Changes
- **Facilities Endpoint**: Now allows PATIENT role to view doctor clinics
- **Doctor Listing**: Removed PMDC verification requirement

---

## 🐛 Bugs Fixed

1. ✅ **Doctor listing showing 0 doctors**
   - Fixed by removing `pmdcVerified` requirement from queries
   
2. ✅ **Clinic listing not working for patients**
   - Fixed by allowing PATIENT role access to facilities endpoint
   
3. ✅ **Token expiration too short (15 minutes)**
   - Extended to 4 hours for better user experience
   
4. ✅ **Clinic status not updating correctly**
   - Fixed in previous session (clinic.active vs clinic.isActive)
   
5. ✅ **Doctor cards not showing all clinics**
   - Now displays all clinics with locations

6. ✅ **Appointment date/time validation error**
   - Fixed "Invalid time value" error by adding date validation and transformation
   - Backend returns `startTime` but frontend expected `appointmentDateTime`
   - Added transformation function to map backend response to frontend format

---

## ⚠️ Known Issues (To Be Fixed)

### 1. **Appointment Type Grouping Issue** 🔴
**Status**: Pending Fix (Friend will handle)

**Problem**:
- When booking an on-site appointment for a specific clinic (e.g., "feroz"), the appointment appears in the "Online Consultations" section on the doctor's appointments page instead of under the selected clinic
- The appointment type shows as "ONSITE" but is incorrectly grouped under "Online Consultations"
- This happens even though the patient selected "On-site Visit" and chose a specific clinic

**Expected Behavior**:
- On-site appointments should appear under their respective clinic section
- Online appointments should appear in "Online Consultations" section

**Root Cause** (Suspected):
- Backend changes to store appointment type in notes field may not be working correctly
- Frontend grouping logic may not be properly extracting appointment type from the stored format
- Backend may need to be restarted for changes to take effect
- The transformation function may not be correctly parsing the appointment type from the notes field

**Files Involved**:
- `healthlink_backend/src/main/java/com/healthlink/domain/appointment/service/AppointmentService.java`
- `healthlink_backend/src/main/java/com/healthlink/domain/appointment/dto/CreateAppointmentRequest.java`
- `frontend/lib/api.ts` (transformation function)
- `frontend/app/doctor/appointments/page.tsx` (grouping logic)

**Note**: Backend stores appointment type in notes field as `APPT_TYPE:ONLINE` or `APPT_TYPE:ONSITE`. The transformation should extract this, but may not be working correctly for existing appointments or new ones.

---

## 🎨 UI/UX Improvements

1. ✅ **Doctor Search Page**
   - Modern design with gradient backgrounds
   - Advanced filtering system
   - Grid and List view modes
   - Real-time search

2. ✅ **Doctor Profile Page**
   - Clean clinic cards with all information
   - Easy booking flow

3. ✅ **Booking Page**
   - Shows clinic hours prominently
   - Pre-fills clinic selection
   - Better visual hierarchy

4. ✅ **Doctor Appointments Page**
   - Grouped by clinic for better organization
   - Appointment counts visible
   - Dual filtering system

---

## 📝 Notes

- All changes are backward compatible
- No database migrations required
- **IMPORTANT**: Token expiration change and clinic endpoint changes require backend restart
  - Run: `cd healthlink_backend && ./gradlew bootRun`
- PMDC verification is optional - doctors can still be approved without it
- Location filtering works by showing only doctors with clinics in selected city
- After restart, patients will be able to view doctor clinics for booking

---

## 🚀 Next Steps

1. **PMDC Verification Frontend** - Create admin/staff portal page for PMDC verification
2. **Payment Gateway Integration** - EasyPaisa/JazzCash integration
3. **Video Call Integration** - Zoom integration
4. **Prescription System** - Basic prescription creation/viewing
5. **Email Notifications** - Critical notifications (appointments, payments)

---

**Last Updated**: 2025-01-06

