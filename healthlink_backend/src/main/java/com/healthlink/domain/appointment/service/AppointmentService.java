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

import com.healthlink.infrastructure.zoom.ZoomApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.healthlink.domain.webhook.EventType;
import com.healthlink.domain.webhook.WebhookPublisherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setFacility(facility);
        appointment.setServiceOffering(serviceOffering);
        appointment.setAppointmentTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.PENDING_PAYMENT);
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
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new RuntimeException("Appointment must be confirmed before check-in");
        }
        Doctor doctor = appointment.getDoctor();
        int earlyCheckInMinutes = doctor.getEarlyCheckinMinutes();
        LocalDateTime now = LocalDateTime.now();
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
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED
                && appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new RuntimeException("Invalid status for staff check-in");
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
        LocalDateTime now = LocalDateTime.now();
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
            if (appointment.getStatus() == AppointmentStatus.IN_PROGRESS) {
                appointment.setStatus(AppointmentStatus.CONFIRMED);
            }
        }
        return mapToResponse(appointmentRepository.save(appointment));
    }

    public AppointmentResponse cancel(UUID appointmentId, String cancelReason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel already completed or cancelled appointment");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setReasonForVisit(appointment.getReasonForVisit() + " [CANCELLED: " + cancelReason + "]");
        Appointment saved = appointmentRepository.save(appointment);
        // Publish cancellation event
        webhookPublisherService.publish(EventType.APPOINTMENT_CANCELED, saved.getId().toString());
        return mapToResponse(saved);
    }

    public AppointmentResponse completeAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            // Allow completing from CONFIRMED if check-in was skipped or implicit
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        return mapToResponse(appointmentRepository.save(appointment));
    }

    public java.util.List<AppointmentResponse> listAppointments(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        java.util.List<Appointment> appointments;
        if (user.getRole() == com.healthlink.domain.user.enums.UserRole.PATIENT) {
            appointments = appointmentRepository.findByPatientId(user.getId());
        } else if (user.getRole() == com.healthlink.domain.user.enums.UserRole.DOCTOR) {
            appointments = appointmentRepository.findByDoctorId(user.getId());
        } else {
            return java.util.Collections.emptyList();
        }

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
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
