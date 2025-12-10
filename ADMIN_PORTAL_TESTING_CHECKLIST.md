
--
## 📊 1. Dashboard Features

### 1.1 Dashboard Access & Layout
- [x] Navigate to `/admin/dashboard`
- [x] Verify dashboard loads without errors
- [x] Verify TopNav is visible with your name and "Admin" role
- [x] Verify Sidebar is visible with navigation items
- [x] Verify page title shows "Platform Overview"

### 1.2 Dashboard Stats Cards
- [x] Verify all 6 stats cards are displayed:
  - [x] Total Patients
  - [x] Total Doctors
  - [x] Active Clinics
  - [x] Total Appointments
  - [x] Pending Appointments
  - [x] Confirmed Appointments
- [x] Verify stats show actual numbers (not all zeros)
- [x] Verify stats update when data changes

### 1.3 Critical Alerts Banner
- [x] Verify red alert banner is visible at top
- [x] Verify banner shows "Critical Alerts" title
- [x] Verify banner displays alert messages
- [x] Verify "View All" button is clickable

### 1.4 Live Activity Feed
- [x] Verify "Live Activity Feed" card is visible
- [x] Verify activity items are displayed
- [x] Verify timestamps are shown (e.g., "2 min ago")
- [x] Verify scrollable area works if many activities

### 1.5 Quick Actions Panel
- [x] Verify "Quick Actions" card is visible
- [x] Verify all 6 quick action buttons are present:
  - [x] Manage Patients
  - [x] Manage Doctors
  - [x] Manage Clinics
  - [x] View Appointments
  - [x] Send Notification
  - [x] System Settings
- [x] Click each button and verify it navigates correctly

### 1.6 Trend Charts
- [x] Verify "User Growth Trend" card is visible
- [x] Verify "Appointment Trends" card is visible
- [x] Verify chart placeholders are displayed
- [x] Verify description text is present

### 1.7 System Status Indicators
- [ x] Verify "System Status" card is visible
- [x ] Verify all 5 services are listed:
  - [ x] API Gateway
  - [x ] Database
  - [ x] Email Service
  - [ x] Payment Gateway
  - [x ] Zoom Integration
- [ x] Verify status badges are displayed (Operational/Degraded/Offline)

### 1.8 Resource Utilization
- [ x] Verify "Resource Utilization" card is visible
- [x ] Verify CPU Usage progress bar is displayed
- [ x] Verify Memory Usage progress bar is displayed
- [ x] Verify Disk Space progress bar is displayed
- [x ] Verify percentages are shown

### 1.9 Dashboard Export/Print
- [x] Click "Print/Export" button
- [x] Verify print dialog opens (or page is prepared for printing)
- [x] Click "Export JSON" button
- [x] Verify JSON file downloads with dashboard data
- [x] Verify downloaded file contains valid JSON

### 1.10 Time Filters
- [x] Verify "Time Filters" card is visible
- [x] Verify filter buttons are displayed: Today, This Week, This Month, All Time
- [x] Verify buttons are clickable (functionality may be placeholder)

---

## 👥 2. User Management (`/admin/users`)

### 2.1 Page Access
- [x] Navigate to `/admin/users`
- [x] Verify page loads without errors
- [x] Verify page title shows "User Management"

### 2.2 Search Functionality
- [x] Type in search box (e.g., "john")
- [x] Verify results filter in real-time
- [x] Verify search works for name, email, phone
- [x] Clear search and verify all users show again

### 2.3 Filters
- [x] Click "Role" filter dropdown
- [ ] Select "Patient" - verify only patients show (ROLE FILTER NOT WORKING - needs fix)
- [ ] Select "Doctor" - verify only doctors show (ROLE FILTER NOT WORKING - needs fix)
- [ ] Select "Admin" - verify only admins show (ROLE FILTER NOT WORKING - needs fix)
- [x] Select "All" - verify all users show
- [x] Test "Status" filter (Active/Suspended/Pending)
- [x] Test "Registration Date" filter

### 2.4 Quick Stats
- [x] Verify 4 stat cards are displayed:
  - [x] Total Users
  - [x] Active Users
  - [x] Pending Approval
  - [x] Suspended Users
- [x] Verify numbers match filtered results

### 2.5 User Table
- [x] Verify table displays columns:
  - [x] Name
  - [x] Email
  - [x] Role (FIXED - now displays correctly)
  - [x] Status
  - [x] Registration Date
  - [x] Actions
- [x] Verify table shows actual user data
- [x] Verify table is scrollable if many users

### 2.6 Multi-Select
- [x] Click checkbox on first user
- [x] Verify checkbox is checked
- [x] Click checkbox on second user
- [x] Verify both are selected
- [x] Verify bulk actions bar appears when users are selected

### 2.7 Bulk Actions
- [x] Select 2-3 users
- [x] Verify bulk actions bar shows selected count
- [ ] Click "Approve Selected" - verify action works (BUTTON NOT VISIBLE - needs implementation)
- [x] Click "Suspend Selected" - verify action works
- [x] Click "Delete Selected" - verify confirmation dialog appears
- [x] Click "Clear Selection" - verify selection clears

### 2.8 Individual User Actions
- [x] Click "View" button on a user
- [x] Verify detail modal/page opens
- [x] Verify detail view shows tabs:
  - [x] Profile
  - [x] Appointments (shows real data with counts)
  - [x] Medical Records (for patients only - shows real data)
  - [x] Prescriptions (for patients only - shows real data)
  - [x] Clinics (for doctors only - FIXED - now loads real data)
  - [x] Audit Trail
- [x] Click each tab and verify content loads
- [x] Close detail view

### 2.9 Approve User
- [ ] Find a user with "Pending" status
- [ ] Click "Approve" button (BUTTON NOT VISIBLE - needs implementation)
- [ ] Verify success message appears
- [ ] Verify user status changes to "Active"

### 2.10 Suspend User
- [ ] Find an active user
- [ ] Click "Suspend" button (BUTTON NOT VISIBLE - needs implementation)
- [ ] Verify confirmation dialog appears
- [ ] Confirm suspension
- [ ] Verify success message appears
- [ ] Verify user status changes to "Suspended"

### 2.11 Delete User
- [x] Click "Delete" button on a user
- [x] Verify confirmation dialog appears
- [x] Confirm deletion
- [x] Verify success message appears
- [x] Verify user is removed from list

### 2.12 CSV Export
- [x] Click "Export CSV" button
- [x] Verify CSV file downloads
- [x] Open CSV file and verify it contains:
  - [x] All visible users
  - [x] Correct columns (Name, Email, Role, Status, etc.)
  - [x] Proper formatting

---

## 👨‍⚕️ 3. Doctor Management (`/admin/doctors`)

### 3.1 Page Access
- [X ] Navigate to `/admin/doctors`
- [ X] Verify page loads without errors
- [ X] Verify page title shows "Doctor Management"

### 3.2 Stats Cards
- [ X] Verify 6 stats cards are displayed:
  - [X ] Total Doctors
  - [X ] Verified
  - [X ] Pending
  - [X ] Suspended
  - [X ] Avg Rating
  - [ X] Total Revenue
- [ X] Verify numbers are accurate

### 3.3 Auto-Approve Rules
- [ X] Verify "Auto-Approve Rules" card is visible
- [ X] Toggle "Enable Auto-Approve" switch
- [ X] Verify switch state changes
- [X ] Set "Min Years Experience" to 5
- [X ] Toggle "Require PMDC" switch
- [X ] Toggle "Require Specialization" switch
- [ X] Verify all settings save

### 3.4 Search & Filters
- [ X] Type in search box
- [ X] Verify results filter by name, email, specialization
- [ X] Test "Verification Status" filter (All/Verified/Pending/Rejected)
- [ X] Test "Account Status" filter (All/Active/Suspended)
- [ X] Click "Clear Filters" - verify all filters reset

### 3.5 Doctor Table
- [ X] Verify table shows:
  - [X ] Name with verification badge
  - [X ] Email
  - [X ] Specialization
  - [ X] PMDC ID
  - [ X] Rating (with star icon)
  - [ X] Experience (years)
  - [ X] Status badge
  - [ X] Action buttons
- [ X] Verify verified doctors show green "Verified" badge

### 3.6 Doctor Actions
- [X ] Click "View" (eye icon) on a doctor
- [X ] Verify detail view opens (if implemented)
- [ X] Click "Verify" (checkmark) on unverified doctor
- [ X] Verify success message
- [ X] Verify doctor gets verified badge
- [ X] Click "Suspend" (ban icon) on a doctor
- [X ] Verify doctor status changes

### 3.7 CSV Export
- [ X] Click "Export CSV" button
- [X ] Verify CSV downloads with doctor data
- [X ] Verify CSV includes all columns

---

## 📅 4. Enhanced Appointment Management (`/admin/appointments/enhanced`)

### 4.1 Page Access
- [ X] Navigate to `/admin/appointments/enhanced`
- [ X] Verify page loads without errors
- [ X] Verify page title shows "Appointment Management"

### 4.2 Revenue Summary
- [ X] Verify 4 revenue cards are displayed:
  - [X ] Total Revenue
  - [X ] Today
  - [X ] This Week
  - [X ] This Month
- [ X] Verify amounts are calculated correctly
- [ X] Verify currency format (PKR)

### 4.3 Conflict Detection
- [X ] Create or find appointments with overlapping times for same doctor
- [ X] Verify red "Scheduling Conflicts Detected" card appears
- [ X] Verify conflict details are shown
- [ X] Verify conflict count is accurate

### 4.4 Bulk Selection
- [ X] Click checkbox on an appointment in list view
- [ X] Verify checkbox is checked
- [X ] Select multiple appointments
- [X ] Verify bulk actions bar appears
- [X ] Verify selected count is shown

### 4.5 Bulk Actions
- [ ] Select 2-3 appointments
- [ ] Click "Send Reminder" - verify action works
- [ ] Click "Reschedule" - verify action works
- [ ] Click "Cancel" - verify confirmation appears
- [ ] Click "Clear Selection" - verify selection clears

### 4.6 View Modes
- [ X] Verify 3 view mode tabs: Calendar, List, Timeline
- [ X] Click "Calendar" tab
- [ X] Verify calendar view displays
- [ X] Click "List" tab
- [ X] Verify list view displays
- [ X] Click "Timeline" tab
- [ X] Verify timeline view displays

### 4.7 Calendar View
- [ X] Verify weekly calendar grid is displayed
- [ X] Verify day names (Mon-Sun) are shown
- [ X] Verify appointments appear on correct days
- [ ] Verify appointment times are shown
- [X ] Click previous/next month buttons
- [X ] Verify calendar navigates correctly
- [X ] Click "Today" button - verify returns to current date

### 4.8 Drag-and-Drop Rescheduling
- [ X] In calendar view, find an appointment
- [ X] Click and drag an appointment to a different day
- [ X] Verify appointment becomes semi-transparent while dragging
- [X ] Drop on a valid day
- [X ] Verify drop zone highlight appears
- [ X] Verify success message appears
- [ it doesnt] Verify appointment moves to new date
- [X ] Try dragging to a day with conflict
- [X ] Verify error message appears (conflict detected)
- [X ] Verify appointment doesn't move

### 4.9 List View
- [ X] Verify appointments are displayed as cards
- [ X] Verify each card shows:
  - [ X] Patient name → Doctor name
  - [ X] Status badge
  - [ X] Appointment type badge
  - [ X] Date & Time
  - [ X] Reason
  - [ X] Clinic name (if applicable)
  - [ X] Action buttons (Remind, Reschedule)
- [ X] Verify checkboxes for bulk selection

### 4.10 Timeline View
- [ X] Verify appointments grouped by date
- [ X] Verify dates are sorted chronologically
- [X ] Verify appointments under each date are sorted by time
- [X ] Verify appointment details are shown

### 4.11 Filters
- [ X] Test "Status" filter dropdown
- [ X] Select "IN_PROGRESS" - verify only in-progress appointments show
- [ X] Select "COMPLETED" - verify only completed show
- [ X] Select "ALL" - verify all appointments show
- [ X] Test other filters if available (doctor, patient, clinic, date range)

### 4.12 Quick Actions
- [X ] Click "Remind" button on an appointment
- [X ] Verify reminder is sent (check for success message)
- [ X] Click "Reschedule" button
- [ X] Verify reschedule dialog/form appears

### 4.13 Export
- [ X] Click "Export" button
- [ X] Verify export functionality works (if implemented)

---

## 🏥 5. Facility Management (`/admin/clinics`)

### 5.1 Page Access
- [ X] Navigate to `/admin/clinics`
- [ X] Verify page loads without errors
- [X ] Verify page title shows "Facility Management"

### 5.2 View Modes
- [X ] Verify "Map View" and "Grid View" buttons
- [X ] Click "Map View"
- [X ] Verify map placeholder is displayed
- [ X] Verify facility count is shown
- [ X] Click "Grid View"
- [ X] Verify grid of facility cards displays

### 5.3 Facility Cards (Grid View)
- [ ] Verify each card shows:
  - [ ] Facility name
  - [ ] Operational status badge (OPERATIONAL/PARTIAL/INACTIVE)
  - [ ] Address
  - [ ] City, State, ZIP
  - [ ] Doctor name
  - [ ] Integration status (Payment Gateway, Video Service)
  - [ ] Delete button
- [ ] Verify status badges have correct colors

### 5.4 Facility Detail Modal
- [X ] Click "View" (eye icon) on a facility
- [ X] Verify modal opens
- [ X] Click "Details" tab
- [X ] Verify address, doctor, consultation fee are shown
- [ X] Click "Operational Status" tab
- [ X] Verify current status and active status are shown
- [ X] Click "Integrations" tab
- [ X] Verify Payment Gateway and Video Service status are shown
- [ X] Close modal

### 5.5 Operational Status
- [ ] Verify status indicators show:
  - [ ] OPERATIONAL (green) - both integrations connected
  - [ ] PARTIAL (yellow) - one integration connected
  - [ ] INACTIVE (gray) - facility inactive or no integrations
- [ ] Verify status matches actual facility state

### 5.6 Integration Status
- [X ] Verify each facility shows:
  - [X ] Payment Gateway connection status
  - [ X] Video Service (Zoom) connection status
- [ X] Verify badges show "Connected" or "Not Connected"

### 5.7 Delete Facility
- [ X] Click "Delete Clinic" on a facility
- [ DX] Verify confirmation dialog appears
- [ X] Confirm deletion
- [X ] Verify success message
- [X ] Verify facility is removed from list

---

## ⚙️ 6. System Settings (`/admin/settings`)

### 6.1 Page Access
- [ ] Navigate to `/admin/settings`
- [ ] Verify page loads without errors
- [ ] Verify page title shows "System Settings"

### 6.2 General Settings Tab
- [ ] Click "General" tab
- [ ] Verify fields are displayed:
  - [ ] Platform Name
  - [ ] Logo (upload)
  - [ ] Timezone
  - [ ] Language
- [ ] Change platform name
- [ ] Click "Save General Settings"
- [ ] Verify success message
- [ ] Refresh page - verify changes persist

### 6.3 Security Settings Tab
- [ ] Click "Security" tab
- [ ] Verify fields are displayed:
  - [ ] Password Min Length
  - [ ] Require Uppercase (toggle)
  - [ ] Require Lowercase (toggle)
  - [ ] Require Numbers (toggle)
  - [ ] Require Special Chars (toggle)
  - [ ] Two-Factor Required (toggle)
  - [ ] Session Timeout (minutes)
- [ ] Toggle switches and verify state changes
- [ ] Change password min length
- [ ] Click "Save Security Settings"
- [ ] Verify success message

### 6.4 Integrations Tab
- [ ] Click "Integrations" tab
- [ ] Verify integration options:
  - [ ] Email Provider
  - [ ] SMS Provider
  - [ ] Payment Gateway
- [ ] Verify "Test" buttons are present
- [ ] Click "Test Email" button
- [ ] Verify test email is sent (check for success message)
- [ ] Click "Test SMS" button (if available)
- [ ] Click "Test Payment" button (if available)

### 6.5 Notifications Tab
- [ ] Click "Notifications" tab
- [ ] Verify toggles for:
  - [ ] Email Enabled
  - [ ] SMS Enabled
  - [ ] Push Enabled
- [ ] Toggle each switch
- [ ] Click "Save Notification Settings"
- [ ] Verify success message

### 6.6 Features Tab
- [ ] Click "Features" tab
- [ ] Verify toggles for:
  - [ ] Telemedicine
  - [ ] Prescriptions
  - [ ] Payments
  - [ ] Analytics
- [ ] Toggle each switch
- [ ] Click "Save Feature Settings"
- [ ] Verify success message

### 6.7 Compliance Tab
- [ ] Click "Compliance" tab
- [ ] Verify fields:
  - [ ] HIPAA Compliance (toggle)
  - [ ] Data Retention (days)
  - [ ] Audit Log Retention (days)
- [ ] Toggle HIPAA compliance
- [ ] Change retention days
- [ ] Click "Save Compliance Settings"
- [ ] Verify success message

### 6.8 Templates Tab
- [ ] Click "Templates" tab
- [ ] Verify "Email Templates" section
- [ ] Click "Preview Email Template" button
- [ ] Verify preview modal opens
- [ ] Verify template preview is displayed
- [ ] Close preview
- [ ] Verify "SMS Templates" section
- [ ] Click "Preview SMS Template" button
- [ ] Verify SMS preview is displayed

### 6.9 Version History Tab
- [ ] Click "Version History" tab
- [ ] Verify version history list is displayed
- [ ] Verify each entry shows:
  - [ ] Version number
  - [ ] Changed by
  - [ ] Changed at (timestamp)
  - [ ] Changes description
- [ ] Make a settings change
- [ ] Return to Version History
- [ ] Verify new entry appears at top

---

## 📊 7. Analytics & Reporting (`/admin/analytics`)

### 7.1 Page Access
- [ ] Navigate to `/admin/analytics`
- [ ] Verify page loads without errors
- [ ] Verify page title shows "Analytics & Reporting"

### 7.2 Overview Tab
- [ ] Verify "Overview" tab is selected by default
- [ ] Verify 4 stat cards are displayed:
  - [ ] Total Users
  - [ ] Total Appointments
  - [ ] Total Revenue
  - [ ] Active Doctors
- [ ] Verify trend indicators (e.g., "+12% from last month")

### 7.3 Time Filter
- [ ] Click time filter dropdown
- [ ] Select "Today" - verify data updates
- [ ] Select "This Week" - verify data updates
- [ ] Select "This Month" - verify data updates
- [ ] Select "All Time" - verify data updates

### 7.4 Export Buttons
- [ ] Click "Export PDF" button
- [ ] Verify PDF export works (or placeholder message)
- [ ] Click "Export Excel" button
- [ ] Verify Excel export works (or placeholder message)

### 7.5 User Growth Tab
- [ ] Click "User Growth" tab
- [ ] Verify user growth chart/visualization is displayed
- [ ] Verify chart shows growth over time

### 7.6 Revenue Tab
- [ ] Click "Revenue" tab
- [ ] Verify revenue chart is displayed
- [ ] Verify revenue breakdown is shown

### 7.7 Appointments Tab
- [ ] Click "Appointments" tab
- [ ] Verify appointment trends chart is displayed
- [ ] Verify appointment statistics are shown

### 7.8 Doctor Performance Tab
- [ ] Click "Doctor Performance" tab
- [ ] Verify doctor performance chart is displayed
- [ ] Verify performance metrics are shown

### 7.9 Custom Report Tab
- [ ] Click "Custom Report" tab
- [ ] Verify custom report builder interface is displayed
- [ ] Verify placeholder or interface is functional

### 7.10 Scheduled Reports Tab
- [ ] Click "Scheduled Reports" tab
- [ ] Verify list of scheduled reports is displayed
- [ ] Verify each report shows:
  - [ ] Report name
  - [ ] Active/Paused status
  - [ ] Frequency (Daily/Weekly/Monthly)
  - [ ] Schedule (day and time)
  - [ ] Email address
  - [ ] Action buttons (Pause/Activate, Delete)

### 7.11 Create Scheduled Report
- [ ] Click "New Scheduled Report" button
- [ ] Verify modal opens
- [ ] Fill in form:
  - [ ] Report Name: "Test Weekly Report"
  - [ ] Frequency: "Weekly"
  - [ ] Day: "Monday"
  - [ ] Time: "09:00"
  - [ ] Email: "admin@healthlink.com"
- [ ] Click "Create" button
- [ ] Verify success message
- [ ] Verify new report appears in list
- [ ] Verify report is marked as "Active"

### 7.12 Manage Scheduled Reports
- [ ] Find an active report
- [ ] Click "Pause" button
- [ ] Verify report status changes to "Paused"
- [ ] Click "Activate" button
- [ ] Verify report status changes to "Active"
- [ ] Click "Delete" (trash icon) button
- [ ] Verify report is removed from list

---

## 🔍 8. Audit & Compliance (`/admin/audit`)

### 8.1 Page Access
- [ ] Navigate to `/admin/audit`
- [ ] Verify page loads without errors
- [ ] Verify page title shows "Audit & Compliance"

### 8.2 Activity Log
- [ ] Verify activity log table/list is displayed
- [ ] Verify columns/fields show:
  - [ ] Timestamp
  - [ ] User/Actor
  - [ ] Action Type
  - [ ] Description
  - [ ] IP Address
  - [ ] Status
- [ ] Verify log entries are sorted by most recent first

### 8.3 Search Functionality
- [ ] Type in search box
- [ ] Verify results filter by user, action type, or description
- [ ] Clear search - verify all entries show again

### 8.4 Filters
- [ ] Test "Action Type" filter (if available)
- [ ] Test "User" filter (if available)
- [ ] Test "Date Range" filter
- [ ] Select start date and end date
- [ ] Verify results filter to date range

### 8.5 Compliance Dashboard
- [ ] Verify compliance status indicators are displayed
- [ ] Verify HIPAA status is shown
- [ ] Verify data breach monitoring status
- [ ] Verify anomaly detection status

### 8.6 Export
- [ ] Click "Export" button
- [ ] Verify export functionality works (CSV/PDF)
- [ ] Verify exported file contains audit log data

---

## 🔔 9. Custom Notifications (`/admin/notifications/new`)

### 9.1 Page Access
- [ ] Navigate to `/admin/notifications/new`
- [ ] Verify page loads without errors
- [ ] Verify page title shows "Send Custom Notification"

### 9.2 Notification Details
- [ ] Enter title: "Test Notification"
- [ ] Verify character counter shows "X/200 characters"
- [ ] Enter message: "This is a test notification message"
- [ ] Verify character counter shows "X/2000 characters"
- [ ] Select notification type: "INFO"
- [ ] Select priority: "MEDIUM"

### 9.3 Templates
- [ ] Click "Templates" button
- [ ] Verify template modal opens
- [ ] Verify pre-built templates are listed:
  - [ ] Appointment Reminder
  - [ ] Welcome Message
  - [ ] System Maintenance
- [ ] Click on a template
- [ ] Verify template is applied (title and message filled)
- [ ] Close template modal

### 9.4 Save as Template
- [ ] Fill in title and message
- [ ] Click "Save as Template" button
- [ ] Verify success message
- [ ] Open templates modal again
- [ ] Verify new template appears in list

### 9.5 Recipient Selection - All Users
- [ ] Select "All Users" as recipient type
- [ ] Verify recipient count shows "All users"

### 9.6 Recipient Selection - All Doctors
- [ ] Select "All Doctors" as recipient type
- [ ] Verify recipient count shows "All doctors"

### 9.7 Recipient Selection - Selected Users (Multi-Select)
- [ ] Select "Selected Users" as recipient type
- [ ] Verify multi-select interface appears
- [ ] Click "Filter" button (verify it's clickable)
- [ ] Check checkbox for User 1
- [ ] Verify checkbox is checked
- [ ] Check checkbox for User 2
- [ ] Verify both are selected
- [ ] Verify "X recipient(s) selected" counter updates
- [ ] Uncheck a user
- [ ] Verify counter decreases

### 9.8 Recipient Selection - Individual User
- [ ] Select "Individual User" as recipient type
- [ ] Verify input field appears
- [ ] Enter user ID
- [ ] Verify recipient count updates

### 9.9 Delivery Channels
- [ ] Verify 4 channel checkboxes:
  - [ ] In-App Notification
  - [ ] Email
  - [ ] SMS
  - [ ] Push Notification
- [ ] Check "In-App Notification"
- [ ] Verify checkbox is checked
- [ ] Check "Email"
- [ ] Verify both are checked
- [ ] Uncheck "In-App Notification"
- [ ] Verify only "Email" is checked
- [ ] Verify at least one channel must be selected

### 9.10 Scheduling
- [ ] Leave scheduled date/time empty
- [ ] Verify "Leave empty to send immediately" message shows
- [ ] Enter future date and time
- [ ] Verify datetime picker works
- [ ] Clear datetime - verify it's empty again

### 9.11 Send Notification
- [ ] Fill in all required fields
- [ ] Select at least one delivery channel
- [ ] Click "Send Notification" button
- [ ] Verify loading state (button disabled)
- [ ] Verify success message appears
- [ ] Verify redirect to notification history page

### 9.12 Preview (if implemented)
- [ ] Fill in title and message
- [ ] Click "Preview" button (if available)
- [ ] Verify preview modal opens
- [ ] Verify notification preview is displayed correctly

---

## 📜 10. Notification History (`/admin/notifications/history`)

### 10.1 Page Access
- [ ] Navigate to `/admin/notifications/history`
- [ ] Verify page loads without errors
- [ ] Verify page title shows "Notification History"

### 10.2 Notification List
- [ ] Verify list of sent notifications is displayed
- [ ] Verify each notification card shows:
  - [ ] Title
  - [ ] Message
  - [ ] Status badge (Sent/Sending/Scheduled/Failed)
  - [ ] Priority badge
  - [ ] Recipient count
  - [ ] Sent count
  - [ ] Delivered count
  - [ ] Failed count
  - [ ] Channels used
  - [ ] Sent timestamp
  - [ ] Created timestamp

### 10.3 Filters
- [ ] Click "Filters" button
- [ ] Verify filter panel opens
- [ ] Test "Status" filter:
  - [ ] Select "Sent" - verify only sent notifications show
  - [ ] Select "Failed" - verify only failed show
  - [ ] Select "ALL" - verify all show
- [ ] Test "Type" filter:
  - [ ] Select "INFO" - verify only info notifications show
  - [ ] Select "ALL" - verify all show
- [ ] Test "Date Range" filters:
  - [ ] Select "From Date"
  - [ ] Select "To Date"
  - [ ] Click "Apply Filters"
  - [ ] Verify results filter to date range
- [ ] Click "Clear" button
- [ ] Verify all filters reset

### 10.4 Delivery Tracking
- [ ] Click "View Delivery Details" on a notification
- [ ] Verify delivery details modal opens
- [ ] Verify modal shows:
  - [ ] Notification title
  - [ ] Sent count (green)
  - [ ] Delivered count (blue)
  - [ ] Failed count (red)
  - [ ] Delivery Status Breakdown by channel:
    - [ ] Email status
    - [ ] SMS status
    - [ ] In-App status
- [ ] Close modal

### 10.5 Pagination
- [ ] If more than 20 notifications, verify pagination controls
- [ ] Click "Next" button
- [ ] Verify next page loads
- [ ] Click "Previous" button
- [ ] Verify previous page loads
- [ ] Verify page number is displayed

---

## ⚡ 11. Automation Rules (`/admin/automation`)

### 11.1 Page Access
- [ ] Navigate to `/admin/automation`
- [ ] Verify page loads without errors
- [ ] Verify page title shows "Automation Rules"

### 11.2 Auto-Approve Doctors Section
- [ ] Verify "Auto-Approve Doctors" section is visible
- [ ] Verify existing rule is displayed (if any)
- [ ] Verify rule shows:
  - [ ] Rule name
  - [ ] Enabled/Disabled toggle
  - [ ] Min Years Experience field
  - [ ] Require PMDC toggle
  - [ ] Require Specialization toggle
  - [ ] Delete button
- [ ] Toggle "Enable Auto-Approve" switch
- [ ] Verify switch state changes
- [ ] Change "Min Years Experience" to 3
- [ ] Toggle "Require PMDC" switch
- [ ] Verify changes are saved

### 11.3 Auto-Suspend Users Section
- [ ] Verify "Auto-Suspend Users" section is visible
- [ ] Verify existing rule is displayed (if any)
- [ ] Verify rule shows:
  - [ ] Rule name
  - [ ] Enabled/Disabled toggle
  - [ ] Inactive Days field
  - [ ] Require No Appointments toggle
  - [ ] Delete button
- [ ] Toggle rule on/off
- [ ] Change "Inactive Days" value
- [ ] Verify changes save

### 11.4 Auto-Send Reminders Section
- [ ] Verify "Auto-Send Reminders" section is visible
- [ ] Verify existing rule is displayed (if any)
- [ ] Verify rule shows:
  - [ ] Rule name
  - [ ] Enabled/Disabled toggle
  - [ ] Reminder Time (hours before) field
  - [ ] Send Email toggle
  - [ ] Send SMS toggle
  - [ ] Delete button
- [ ] Toggle rule on/off
- [ ] Change reminder time
- [ ] Toggle email/SMS channels
- [ ] Verify changes save

### 11.5 Create New Rule
- [ ] Click "New Rule" button
- [ ] Verify modal opens
- [ ] Fill in form:
  - [ ] Rule Name: "Test Auto-Approve Rule"
  - [ ] Rule Type: "Auto-Approve Doctor"
  - [ ] Enabled: Checked
- [ ] Click "Save" button
- [ ] Verify success message
- [ ] Verify new rule appears in appropriate section
- [ ] Click "Cancel" button (test cancel functionality)

### 11.6 Delete Rule
- [ ] Click delete (trash icon) on a rule
- [ ] Verify confirmation dialog appears
- [ ] Confirm deletion
- [ ] Verify success message
- [ ] Verify rule is removed

---

## 💡 12. Smart Suggestions (`/admin/smart-suggestions`)

### 12.1 Page Access
- [ ] Navigate to `/admin/smart-suggestions`
- [ ] Verify page loads without errors
- [ ] Verify page title shows "Smart Suggestions"

### 12.2 Summary Cards
- [ ] Verify 3 summary cards:
  - [ ] High Priority count
  - [ ] Action Required count
  - [ ] Total Suggestions count
- [ ] Verify numbers are accurate

### 12.3 Filters
- [ ] Verify filter tabs:
  - [ ] All Suggestions
  - [ ] High Priority
  - [ ] Action Required
- [ ] Click "High Priority" tab
- [ ] Verify only high priority suggestions show
- [ ] Click "Action Required" tab
- [ ] Verify only action-required suggestions show
- [ ] Click "All Suggestions" tab
- [ ] Verify all suggestions show

### 12.4 Suggestions List
- [ ] Verify suggestions are displayed as cards
- [ ] Verify each suggestion shows:
  - [ ] Type icon (Clock/AlertTriangle/TrendingUp)
  - [ ] Title
  - [ ] Description
  - [ ] Priority badge (High/Medium/Low)
  - [ ] Action Required badge (if applicable)
  - [ ] "Take Action" button (if action required)
  - [ ] "Dismiss" button

### 12.5 License Renewal Suggestions
- [ ] Find a "License Renewal" suggestion
- [ ] Verify it shows doctor count and days until expiry
- [ ] Click "Take Action" button
- [ ] Verify action is triggered (or navigates to relevant page)

### 12.6 Anomaly Detection Suggestions
- [ ] Find an "Anomaly Detection" suggestion
- [ ] Verify it shows anomaly details (e.g., cancellation rate increase)
- [ ] Click "Take Action" button
- [ ] Verify action works

### 12.7 Peak Hours Suggestions
- [ ] Find a "Peak Hours" suggestion
- [ ] Verify it shows peak time range and utilization
- [ ] Verify "Dismiss" button is available

### 12.8 Dismiss Suggestion
- [ ] Click "Dismiss" button on a suggestion
- [ ] Verify success message
- [ ] Verify suggestion is removed from list
- [ ] Verify summary card counts update

---

## 🔍 13. Enhanced Navigation (TopNav)

### 13.1 Global Search
- [ ] Verify search bar is visible in TopNav (for admin)
- [ ] Click on search bar
- [ ] Type "test" in search
- [ ] Verify search functionality works (or placeholder)
- [ ] Press Cmd/Ctrl+K
- [ ] Verify search bar focuses
- [ ] Press Escape
- [ ] Verify search closes

### 13.2 Notifications Dropdown
- [ ] Click bell icon in TopNav
- [ ] Verify notifications dropdown opens
- [ ] Verify unread count badge is shown (if notifications exist)
- [ ] Verify notification list is displayed
- [ ] Click on a notification
- [ ] Verify notification is marked as read (or navigates)
- [ ] Click outside dropdown
- [ ] Verify dropdown closes

### 13.3 User Menu
- [ ] Click on user avatar/name in TopNav
- [ ] Verify dropdown menu opens
- [ ] Verify menu shows:
  - [ ] User name and role
  - [ ] Profile link
  - [ ] Settings link
  - [ ] Dashboard link (for admin)
  - [ ] Logout button
- [ ] Click "Profile"
- [ ] Verify navigation works (or placeholder)
- [ ] Click "Settings"
- [ ] Verify navigates to `/admin/settings`
- [ ] Click "Dashboard"
- [ ] Verify navigates to `/admin/dashboard`
- [ ] Click "Logout"
- [ ] Verify logout works

---

## 🎯 14. Sidebar Navigation

### 14.1 Sidebar Items
- [ ] Verify sidebar shows all navigation items:
  - [ ] Dashboard
  - [ ] Users
  - [ ] Doctors
  - [ ] Appointments
  - [ ] Clinics
  - [ ] Settings
  - [ ] Analytics
  - [ ] Audit
  - [ ] Notifications
  - [ ] Automation
  - [ ] Smart Suggestions
- [ ] Verify current page is highlighted

### 14.2 Navigation
- [ ] Click each sidebar item
- [ ] Verify navigation works correctly
- [ ] Verify page loads without errors
- [ ] Verify active state updates

---

## 🔒 15. Security & OTP Fix

### 15.1 OTP Verification (Backend)
- [ ] Sign up as a new patient
- [ ] Receive OTP email (e.g., code: 123456)
- [ ] Enter correct OTP
- [ ] Verify account is verified successfully
- [ ] Sign up as another patient
- [ ] Receive OTP email (e.g., code: 789012)
- [ ] Enter WRONG OTP (e.g., 000000)
- [ ] Verify verification FAILS (does not accept wrong code)
- [ ] Enter correct OTP
- [ ] Verify verification succeeds

---

## ✅ Final Verification

### Overall Functionality
- [ ] All pages load without errors
- [ ] No console errors in browser DevTools
- [ ] All API calls return expected data
- [ ] All buttons and links work correctly
- [ ] All forms validate input correctly
- [ ] All modals open and close properly
- [ ] All exports/downloads work
- [ ] All filters and searches work
- [ ] All bulk actions work
- [ ] All individual actions work

### Data Accuracy
- [ ] Stats on dashboard match actual data
- [ ] User counts are accurate
- [ ] Appointment counts are accurate
- [ ] Revenue calculations are correct
- [ ] Filter results are accurate

### User Experience
- [ ] Loading states are shown for async operations
- [ ] Success messages appear after actions
- [ ] Error messages are clear and helpful
- [ ] Confirmations appear for destructive actions
- [ ] Navigation is smooth and intuitive

---

## 📝 Notes

- Test each feature thoroughly before marking as complete
- If a feature doesn't work, note the issue and move to the next test
- Some features may require backend data to test properly
- Some features may be placeholders that need backend integration

---

**Total Tests:** ~150+ individual test cases
**Estimated Time:** 2 hours (as requested)

**Start Testing:** Begin with Dashboard Features (Section 1) and work through each section sequentially.

