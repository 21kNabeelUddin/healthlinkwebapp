package com.healthlink.domain.appointment.service;

import com.healthlink.domain.appointment.dto.AppointmentResponse;
import com.healthlink.domain.appointment.dto.CreateAppointmentRequest;
import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.organization.entity.Facility;
import com.healthlink.domain.organization.entity.ServiceOffering;
import com.healthlink.domain.organization.repository.FacilityRepository;
import com.healthlink.domain.organization.repository.ServiceOfferingRepository;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.entity.Staff;
import com.healthlink.domain.user.repository.DoctorRepository;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.domain.notification.NotificationType;
import com.healthlink.domain.notification.service.NotificationSchedulerService;

import com.healthlink.infrastructure.zoom.ZoomApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.healthlink.domain.webhook.EventType;
import com.healthlink.domain.webhook.WebhookPublisherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j

public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final FacilityRepository facilityRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final StaffAssignmentService staffAssignmentService;
    private final WebhookPublisherService webhookPublisherService;

    private final ZoomApiService zoomApiService;
    private final com.healthlink.service.notification.EmailService emailService;
    private final NotificationSchedulerService notificationSchedulerService;


    public AppointmentResponse createAppointment(CreateAppointmentRequest request, String patientEmail) {
        Patient patient = (Patient) userRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new RuntimeException("Facility not found"));
        validateFacilityOwnership(doctor, facility);

        ServiceOffering serviceOffering = null;
        if (request.getServiceOfferingId() != null) {
            serviceOffering = serviceOfferingRepository.findById(request.getServiceOfferingId())
                    .orElseThrow(() -> new RuntimeException("Service offering not found"));
            if (!serviceOffering.getFacility().getId().equals(facility.getId())) {
                throw new RuntimeException("Service offering does not belong to facility");
            }
        }

        int duration = resolveDurationMinutes(doctor, serviceOffering);
        LocalDateTime startTime = request.getAppointmentTime();
        LocalDateTime endTime = startTime.plusMinutes(duration);

        // Validate within facility hours and slot alignment
        LocalTime opening = parseTimeOrDefault(facility.getOpeningTime(), LocalTime.of(9, 0));
        LocalTime closing = parseTimeOrDefault(facility.getClosingTime(), LocalTime.of(17, 0));
        if (!closing.isAfter(opening)) {
            throw new RuntimeException("Clinic closing time must be after opening time");
        }
        LocalDate appointmentDate = startTime.toLocalDate();
        LocalDateTime windowStart = appointmentDate.atTime(opening);
        LocalDateTime windowEnd = appointmentDate.atTime(closing);
        if (startTime.isBefore(windowStart) || endTime.isAfter(windowEnd)) {
            throw new RuntimeException("Appointment time must be within clinic working hours");
        }

        int slotMinutes = (doctor.getSlotDurationMinutes() != null && doctor.getSlotDurationMinutes() > 0)
                ? doctor.getSlotDurationMinutes()
                : 15;
        long minutesFromOpen = Duration.between(windowStart, startTime).toMinutes();
        if (minutesFromOpen % slotMinutes != 0) {
            throw new RuntimeException("Please select a valid time slot");
        }
        
        // Validate appointment time for emergency appointments
        Boolean isEmergency = request.getIsEmergency() != null && request.getIsEmergency();
        if (isEmergency) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fiveMinutesFromNow = now.plusMinutes(5);
            // Emergency appointments can be scheduled for current time or future (within 5 minutes)
            if (startTime.isBefore(now)) {
                throw new RuntimeException("Emergency appointment time cannot be in the past");
            }
            if (startTime.isAfter(fiveMinutesFromNow)) {
                throw new RuntimeException("Emergency appointments must be scheduled within 5 minutes from now");
            }
        } else {
            // Non-emergency appointments must be in the future
            if (startTime.isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Appointment time must be in the future");
            }
        }

        // Check for overlaps - skip for emergency appointments
        // RULE: No buffers allowed. Consecutive appointments can share a boundary
        // (e.g., 10:00-10:30, 10:30-11:00).
        // The repository query uses strict inequality for overlap: (StartA < EndB) AND
        // (EndA > StartB)
        // Emergency appointments bypass availability checks - doctor is always available
        if (!isEmergency && appointmentRepository.existsOverlappingAppointment(doctor.getId(), startTime, endTime)) {
            throw new RuntimeException("Doctor is not available at this time");
        }
        if (!isEmergency) {
            var facilityConflicts = appointmentRepository.findByFacilityIdAndAppointmentTimeBetween(
                    facility.getId(), startTime, endTime);
            boolean hasActive = facilityConflicts.stream()
                    .anyMatch(a -> a.getStatus() != AppointmentStatus.CANCELLED);
            if (hasActive) {
                throw new RuntimeException("This time slot is already booked");
            }
        }

        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setFacility(facility);
        appointment.setServiceOffering(serviceOffering);
        appointment.setAppointmentTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointment.setReasonForVisit(request.getReasonForVisit());
        appointment.setIsEmergency(isEmergency);
        
        // Store appointment type in notes field temporarily (format: "APPT_TYPE:ONLINE" or "APPT_TYPE:ONSITE")
        // This allows us to retrieve it later without a database migration
        String appointmentType = request.getType() != null ? request.getType() : "ONSITE";
        String existingNotes = request.getNotes() != null ? request.getNotes() : "";
        appointment.setNotes("APPT_TYPE:" + appointmentType + (existingNotes.isEmpty() ? "" : "|" + existingNotes));

        if (requiresStaffAssignment(facility, serviceOffering)) {
            var staff = staffAssignmentService.assignStaff(facility.getId(), startTime, endTime);
            appointment.setAssignedStaff(staff);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Create Zoom meeting for ONLINE appointments
        if ("ONLINE".equalsIgnoreCase(appointmentType)) {
            createZoomMeeting(savedAppointment, doctor, patient, startTime, duration);
        }


        // Emit webhook event for downstream integrations (CRM / analytics)
        webhookPublisherService.publish(EventType.APPOINTMENT_CREATED, savedAppointment.getId().toString());
        return mapToResponse(savedAppointment);
    }

    public AppointmentResponse getAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        return mapToResponse(appointment);
    }

    // Phase 2: patient-only check-in (early window guard)
    public AppointmentResponse patientCheckIn(UUID appointmentId, String patientEmail) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (!appointment.getPatient().getEmail().equals(patientEmail)) {
            throw new RuntimeException("Unauthorized patient");
        }
        // Allow check-in if appointment time has started or is in progress
        LocalDateTime now = LocalDateTime.now();
        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
            appointment.getStatus() == AppointmentStatus.CANCELLED ||
            appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new RuntimeException("Cannot check in to a " + appointment.getStatus() + " appointment");
        }
        // Auto-set to IN_PROGRESS if appointment time has started
        if (now.isAfter(appointment.getAppointmentTime()) && appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        }
        Doctor doctor = appointment.getDoctor();
        int earlyCheckInMinutes = doctor.getEarlyCheckinMinutes();
        if (now.isBefore(appointment.getAppointmentTime().minusMinutes(earlyCheckInMinutes))) {
            throw new RuntimeException("Too early to check in. Please wait.");
        }
        boolean staffRequired = requiresStaffAssignment(appointment.getFacility(), appointment.getServiceOffering());
        if (staffRequired && appointment.getAssignedStaff() == null) {
            throw new RuntimeException("Staff assignment required before check-in");
        }
        appointment.setPatientCheckInTime(now);
        updateStatusBasedOnCheckIns(appointment, now);
        return mapToResponse(appointmentRepository.save(appointment));
    }

    // Phase 2: staff check-in (requires patient check-in first or marks readiness)
    public AppointmentResponse staffCheckIn(UUID appointmentId, UUID staffUserId, boolean actorIsDoctor) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        // Allow staff check-in if appointment time has started
        LocalDateTime now = LocalDateTime.now();
        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
            appointment.getStatus() == AppointmentStatus.CANCELLED ||
            appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new RuntimeException("Cannot check in to a " + appointment.getStatus() + " appointment");
        }
        // Auto-set to IN_PROGRESS if appointment time has started
        if (now.isAfter(appointment.getAppointmentTime()) && appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        }
        Staff assignedStaff = appointment.getAssignedStaff();
        if (assignedStaff == null) {
            throw new RuntimeException("No staff is assigned to this appointment");
        }
        if (!actorIsDoctor) {
            if (staffUserId == null || !assignedStaff.getId().equals(staffUserId)) {
                throw new RuntimeException("Only the assigned staff member can check in");
            }
        }
        appointment.setStaffCheckInTime(now);
        updateStatusBasedOnCheckIns(appointment, now);
        return mapToResponse(appointmentRepository.save(appointment));
    }

    // Phase 2: reschedule (allowed only if not completed/cancelled)
    public AppointmentResponse reschedule(UUID appointmentId, LocalDateTime newStartTime, String patientEmail) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // IDOR Check
        if (!appointment.getPatient().getEmail().equals(patientEmail)) {
            throw new RuntimeException("Unauthorized: Appointment does not belong to this patient");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new RuntimeException("Cannot reschedule completed or cancelled appointment");
        }
        var doctor = appointment.getDoctor();
        LocalDateTime newEndTime = newStartTime.plusMinutes(doctor.getSlotDurationMinutes());
        if (appointmentRepository.existsOverlappingAppointment(doctor.getId(), newStartTime, newEndTime)) {
            throw new RuntimeException("Doctor is not available at the new time");
        }
        appointment.setAppointmentTime(newStartTime);
        appointment.setEndTime(newEndTime);
        if (requiresStaffAssignment(appointment.getFacility(), appointment.getServiceOffering())) {
            if (appointment.getFacility() == null) {
                throw new RuntimeException("Facility information is required for staff assignment");
            }
            Staff staff = appointment.getAssignedStaff();
            boolean needsReassignment = staff == null
                    || appointmentRepository.staffHasConflictingAppointment(staff.getId(), newStartTime, newEndTime,
                            appointment.getId());
            if (needsReassignment) {
                staff = staffAssignmentService.assignStaff(appointment.getFacility().getId(), newStartTime, newEndTime,
                        appointment.getId());
                appointment.setAssignedStaff(staff);
            }
            appointment.setPatientCheckInTime(null);
            appointment.setStaffCheckInTime(null);
            appointment.setIsCheckedIn(false);
            appointment.setCheckInTime(null);
            // Status remains IN_PROGRESS when patient checks out early
        }
        return mapToResponse(appointmentRepository.save(appointment));
    }

    public AppointmentResponse cancel(UUID appointmentId, String cancelReason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new RuntimeException("Cannot cancel already completed, cancelled, or no-show appointment");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setReasonForVisit(appointment.getReasonForVisit() + " [CANCELLED: " + cancelReason + "]");
        Appointment saved = appointmentRepository.save(appointment);
        // Publish cancellation event
        webhookPublisherService.publish(EventType.APPOINTMENT_CANCELED, saved.getId().toString());
        return mapToResponse(saved);
    }

    public AppointmentResponse markNoShow(UUID appointmentId, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        
        // Only allow marking as no show if appointment time has passed and meeting duration has elapsed
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointmentEndTime = appointment.getEndTime();
        
        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new RuntimeException("Cannot mark as no-show: appointment is already " + appointment.getStatus());
        }
        
        if (now.isBefore(appointmentEndTime)) {
            throw new RuntimeException("Cannot mark as no-show: appointment time has not elapsed yet");
        }
        
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        String noShowReason = reason != null ? reason : "Patient did not show up";
        appointment.setReasonForVisit(appointment.getReasonForVisit() + " [NO_SHOW: " + noShowReason + "]");
        Appointment saved = appointmentRepository.save(appointment);
        // Publish no-show event
        webhookPublisherService.publish(EventType.APPOINTMENT_CANCELED, saved.getId().toString());
        return mapToResponse(saved);
    }

    public AppointmentResponse completeAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new RuntimeException("Appointment must be in progress to complete");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        return mapToResponse(appointmentRepository.save(appointment));
    }

    public AppointmentResponse rescheduleAppointment(UUID appointmentId, java.time.LocalDateTime newStartTime) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (newStartTime == null) {
            throw new RuntimeException("New start time is required");
        }

        // Keep same duration
        int durationMinutes = 30;
        if (appointment.getEndTime() != null && appointment.getAppointmentTime() != null) {
            durationMinutes = (int) java.time.Duration.between(appointment.getAppointmentTime(), appointment.getEndTime()).toMinutes();
            if (durationMinutes <= 0) {
                durationMinutes = 30;
            }
        }

        appointment.setAppointmentTime(newStartTime);
        appointment.setEndTime(newStartTime.plusMinutes(durationMinutes));

        Appointment saved = appointmentRepository.save(appointment);
        sendRescheduleNotifications(saved);
        return mapToResponse(saved);
    }

    private void sendRescheduleNotifications(Appointment appointment) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");
        String friendlyTime = appointment.getAppointmentTime().format(formatter);

        // Patient notification
        if (appointment.getPatient() != null) {
            try {
                notificationSchedulerService.scheduleNotification(
                        appointment.getPatient().getId(),
                        NotificationType.APPOINTMENT_CONFIRMED,
                        "Appointment rescheduled",
                        "Your appointment has been rescheduled to " + friendlyTime
                );

                if (appointment.getPatient().getEmail() != null) {
                    emailService.sendSimpleEmail(
                            appointment.getPatient().getEmail(),
                            "Your appointment was rescheduled",
                            "Your appointment with " +
                                    (appointment.getDoctor() != null ? appointment.getDoctor().getFullName() : "the doctor") +
                                    " has been rescheduled to " + friendlyTime + "."
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to send patient reschedule notification for appointment {}", appointment.getId(), e);
            }
        }

        // Doctor notification
        if (appointment.getDoctor() != null) {
            try {
                notificationSchedulerService.scheduleNotification(
                        appointment.getDoctor().getId(),
                        NotificationType.APPOINTMENT_CONFIRMED,
                        "Appointment rescheduled",
                        "Appointment with " +
                                (appointment.getPatient() != null ? appointment.getPatient().getFullName() : "patient") +
                                " moved to " + friendlyTime
                );

                if (appointment.getDoctor().getEmail() != null) {
                    emailService.sendSimpleEmail(
                            appointment.getDoctor().getEmail(),
                            "Appointment rescheduled",
                            "Your appointment with " +
                                    (appointment.getPatient() != null ? appointment.getPatient().getFullName() : "patient") +
                                    " has been rescheduled to " + friendlyTime + "."
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to send doctor reschedule notification for appointment {}", appointment.getId(), e);
            }
        }
    }

    public java.util.List<AppointmentResponse> listAppointments(String email, String status) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        java.util.List<Appointment> appointments;
        // Use native queries that filter invalid statuses to prevent deserialization errors
        if (user.getRole() == com.healthlink.domain.user.enums.UserRole.PATIENT) {
            appointments = appointmentRepository.findByPatientIdWithValidStatus(user.getId());
        } else if (user.getRole() == com.healthlink.domain.user.enums.UserRole.DOCTOR) {
            appointments = appointmentRepository.findByDoctorIdWithValidStatus(user.getId());
        } else {
            return java.util.Collections.emptyList();
        }

        // Auto-update status to IN_PROGRESS for appointments that have started
        // Only update if not already in a final state
        LocalDateTime now = LocalDateTime.now();
        for (Appointment apt : appointments) {
            // Only auto-update if appointment time has started and status is not final
            if (apt.getStatus() != AppointmentStatus.IN_PROGRESS &&
                apt.getStatus() != AppointmentStatus.COMPLETED &&
                apt.getStatus() != AppointmentStatus.CANCELLED &&
                apt.getStatus() != AppointmentStatus.NO_SHOW &&
                now.isAfter(apt.getAppointmentTime())) {
                apt.setStatus(AppointmentStatus.IN_PROGRESS);
                appointmentRepository.save(apt);
            }
        }

        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            try {
                AppointmentStatus statusEnum = AppointmentStatus.valueOf(status);
                appointments = appointments.stream()
                        .filter(apt -> apt.getStatus() == statusEnum)
                        .collect(java.util.stream.Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Invalid status, return all appointments
            }
        }

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public java.util.List<AppointmentResponse> listAllAppointments(String status) {
        java.util.List<Appointment> appointments = appointmentRepository.findAllWithValidStatus();
        
        // Auto-update status to IN_PROGRESS for appointments that have started
        LocalDateTime now = LocalDateTime.now();
        for (Appointment apt : appointments) {
            if (apt.getStatus() != AppointmentStatus.IN_PROGRESS &&
                apt.getStatus() != AppointmentStatus.COMPLETED &&
                apt.getStatus() != AppointmentStatus.CANCELLED &&
                apt.getStatus() != AppointmentStatus.NO_SHOW &&
                now.isAfter(apt.getAppointmentTime())) {
                apt.setStatus(AppointmentStatus.IN_PROGRESS);
                appointmentRepository.save(apt);
            }
        }

        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            try {
                AppointmentStatus statusEnum = AppointmentStatus.valueOf(status);
                appointments = appointments.stream()
                        .filter(apt -> apt.getStatus() == statusEnum)
                        .collect(java.util.stream.Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Invalid status, return all appointments
            }
        }

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    public void sendReminder(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        String patientEmail = appointment.getPatient().getEmail();
        String patientName = appointment.getPatient().getFullName();
        String doctorName = appointment.getDoctor().getFullName();
        String timeStr = appointment.getAppointmentTime() != null
                ? appointment.getAppointmentTime().toString()
                : "your scheduled time";
        emailService.sendAppointmentReminder(patientEmail, patientName, doctorName, timeStr);
    }

    private void updateStatusBasedOnCheckIns(Appointment appointment, LocalDateTime eventTime) {
        boolean staffRequired = requiresStaffAssignment(appointment.getFacility(), appointment.getServiceOffering());
        if (!staffRequired) {
            appointment.setStatus(AppointmentStatus.IN_PROGRESS);
            appointment.setIsCheckedIn(true);
            appointment.setCheckInTime(eventTime);
            return;
        }
        if (appointment.getPatientCheckInTime() != null && appointment.getStaffCheckInTime() != null) {
            appointment.setStatus(AppointmentStatus.IN_PROGRESS);
            appointment.setIsCheckedIn(true);
            appointment.setCheckInTime(appointment.getPatientCheckInTime());
        }
    }

    private int resolveDurationMinutes(Doctor doctor, ServiceOffering serviceOffering) {
        if (serviceOffering != null && serviceOffering.getDurationMinutes() != null
                && serviceOffering.getDurationMinutes() > 0) {
            return serviceOffering.getDurationMinutes();
        }
        return doctor.getSlotDurationMinutes() != null ? doctor.getSlotDurationMinutes() : 15;
    }

    private LocalTime parseTimeOrDefault(String value, LocalTime fallback) {
        try {
            return value != null ? LocalTime.parse(value) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private void validateFacilityOwnership(Doctor doctor, Facility facility) {
        if (facility == null) {
            throw new RuntimeException("Facility is required");
        }
        if (!facility.isActive()) {
            throw new RuntimeException("Facility is not active");
        }
        if (facility.getDoctorOwner() != null && !facility.getDoctorOwner().getId().equals(doctor.getId())) {
            throw new RuntimeException("Doctor does not manage the selected facility");
        }
    }

    private boolean requiresStaffAssignment(Facility facility, ServiceOffering serviceOffering) {
        if (serviceOffering != null && Boolean.TRUE.equals(serviceOffering.getRequiresStaffAssignment())) {
            return true;
        }
        return facility != null && Boolean.TRUE.equals(facility.getRequiresStaffAssignment());
    }


    /**
     * Create Zoom meeting for online appointments
     */
    private void createZoomMeeting(Appointment appointment, Doctor doctor, Patient patient, 
                                   LocalDateTime startTime, int durationMinutes) {
        try {
            String topic = String.format("Consultation: Dr. %s - %s", 
                    doctor.getFullName(), patient.getFullName());
            
            ZoomApiService.CreateZoomMeetingRequest zoomRequest = 
                    ZoomApiService.CreateZoomMeetingRequest.builder()
                    .topic(topic)
                    .startTime(startTime)
                    .durationMinutes(durationMinutes)
                    .timezone("UTC")
                    .build();

            ZoomApiService.ZoomMeetingResponse zoomMeeting = zoomApiService.createMeeting(zoomRequest);
            
            if (zoomMeeting != null) {
                appointment.setZoomMeetingId(String.valueOf(zoomMeeting.getId()));
                appointment.setZoomMeetingUrl(zoomMeeting.getJoinUrl());
                appointment.setZoomMeetingPassword(zoomMeeting.getPassword());
                appointment.setZoomJoinUrl(zoomMeeting.getJoinUrl());
                appointment.setZoomStartUrl(zoomMeeting.getStartUrl());
                
                appointmentRepository.save(appointment);
                log.info("Zoom meeting created for appointment: {}", appointment.getId());
            } else {
                log.warn("Failed to create Zoom meeting for appointment: {}", appointment.getId());
            }
        } catch (Exception e) {
            log.error("Error creating Zoom meeting for appointment: {}", appointment.getId(), e);
            // Don't fail appointment creation if Zoom fails
        }
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        // Extract payment details if payment exists
        java.math.BigDecimal fee = null;
        Boolean isPaid = false;
        if (appointment.getPayment() != null) {
            fee = appointment.getPayment().getAmount();
            isPaid = appointment.getPayment()
                    .getStatus() == com.healthlink.domain.appointment.entity.PaymentStatus.CAPTURED;
        }

        // Extract appointment type from notes field (format: "APPT_TYPE:ONLINE" or "APPT_TYPE:ONSITE")
        String appointmentType = "ONSITE"; // Default
        if (appointment.getNotes() != null && appointment.getNotes().startsWith("APPT_TYPE:")) {
            String typePart = appointment.getNotes().split("\\|")[0];
            appointmentType = typePart.replace("APPT_TYPE:", "");
        } else if (appointment.getFacility() == null) {
            // If no facility, it's likely an online appointment
            appointmentType = "ONLINE";
        }
        
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatient().getId().toString())
                .doctorId(appointment.getDoctor().getId().toString())
                .facilityId(appointment.getFacility() != null ? appointment.getFacility().getId() : null)
                .serviceOfferingId(
                        appointment.getServiceOffering() != null ? appointment.getServiceOffering().getId() : null)
                .assignedStaffId(appointment.getAssignedStaff() != null ? appointment.getAssignedStaff().getId() : null)
                .staffAssignmentRequired(
                        requiresStaffAssignment(appointment.getFacility(), appointment.getServiceOffering()))
                .startTime(appointment.getAppointmentTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus().name())
                .type(appointmentType)
                .notes(appointment.getReasonForVisit())
                .patientCheckInTime(appointment.getPatientCheckInTime())
                .staffCheckInTime(appointment.getStaffCheckInTime())
                .fee(fee)
                .isPaid(isPaid)
                .isEmergency(appointment.getIsEmergency() != null ? appointment.getIsEmergency() : false)

                .zoomMeetingId(appointment.getZoomMeetingId())
                .zoomMeetingUrl(appointment.getZoomMeetingUrl())
                .zoomMeetingPassword(appointment.getZoomMeetingPassword())
                .zoomJoinUrl(appointment.getZoomJoinUrl())
                .zoomStartUrl(appointment.getZoomStartUrl())

                // Enriched data for frontend display
                .patientName(appointment.getPatient().getFullName())
                .patientEmail(appointment.getPatient().getEmail())
                .doctorName(appointment.getDoctor().getFullName())
                .doctorSpecialization(appointment.getDoctor().getSpecialization())
                .clinicName(appointment.getFacility() != null ? appointment.getFacility().getName() : null)
                .clinicAddress(appointment.getFacility() != null ? 
                    (appointment.getFacility().getAddress() + ", " + appointment.getFacility().getCity()) : null)

                .build();
    }
}
