package com.healthlink.domain.appointment.controller;

import com.healthlink.domain.appointment.dto.CreateAppointmentRequest;
import com.healthlink.domain.appointment.dto.AppointmentResponse;
import com.healthlink.domain.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Book an appointment")
    @ApiResponse(responseCode = "200", description = "Appointment booked successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(appointmentService.createAppointment(request, authentication.getName()));
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('PATIENT', 'STAFF', 'DOCTOR')")
    @Operation(summary = "Check in for an appointment")
    @ApiResponse(responseCode = "200", description = "Check-in successful")
    @ApiResponse(responseCode = "404", description = "Appointment not found")
    public ResponseEntity<AppointmentResponse> checkIn(@PathVariable java.util.UUID id, Authentication authentication) {
        boolean isPatient = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        boolean isDoctor = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));

        if (isPatient) {
            return ResponseEntity.ok(appointmentService.patientCheckIn(id, authentication.getName()));
        } else {
            // Staff or Doctor
            java.util.UUID staffId = null;
            if (authentication.getPrincipal() instanceof com.healthlink.security.model.CustomUserDetails cud) {
                staffId = cud.getId();
            }
            return ResponseEntity.ok(appointmentService.staffCheckIn(id, staffId, isDoctor));
        }
    }

    @PostMapping("/{id}/patient-check-in")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Patient check-in (split logic)")
    @ApiResponse(responseCode = "200", description = "Check-in successful")
    public ResponseEntity<AppointmentResponse> patientCheckIn(@PathVariable java.util.UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(appointmentService.patientCheckIn(id, authentication.getName()));
    }

    @PostMapping("/{id}/staff-check-in")
    @PreAuthorize("hasAnyRole('STAFF','DOCTOR')")
    @Operation(summary = "Staff check-in (split logic)")
    @ApiResponse(responseCode = "200", description = "Check-in successful")
    public ResponseEntity<AppointmentResponse> staffCheckIn(@PathVariable java.util.UUID id,
            Authentication authentication) {
        java.util.UUID staffId = null;
        if (authentication.getPrincipal() instanceof com.healthlink.security.model.CustomUserDetails cud) {
            staffId = cud.getId();
        }
        boolean isDoctor = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        return ResponseEntity.ok(appointmentService.staffCheckIn(id, staffId, isDoctor));
    }

    @PostMapping("/reschedule")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Reschedule an appointment (patient initiated)")
    @ApiResponse(responseCode = "200", description = "Reschedule successful")
    public ResponseEntity<AppointmentResponse> reschedule(
            @Valid @RequestBody com.healthlink.domain.appointment.dto.RescheduleAppointmentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(appointmentService.reschedule(request.getAppointmentId(), request.getNewStartTime(),
                authentication.getName()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','STAFF')")
    @Operation(summary = "Cancel an appointment")
    @ApiResponse(responseCode = "200", description = "Cancellation successful")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable java.util.UUID id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity
                .ok(appointmentService.cancel(id, reason != null ? reason : "User requested cancellation"));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR','STAFF')")
    @Operation(summary = "Complete an appointment")
    @ApiResponse(responseCode = "200", description = "Completion successful")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(appointmentService.completeAppointment(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','STAFF','ORGANIZATION','ADMIN')")
    @Operation(summary = "Get appointment by ID")
    @ApiResponse(responseCode = "200", description = "Appointment found")
    @ApiResponse(responseCode = "404", description = "Appointment not found")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(appointmentService.getAppointment(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @Operation(summary = "List appointments")
    @ApiResponse(responseCode = "200", description = "List of appointments")
    public ResponseEntity<java.util.List<AppointmentResponse>> listAppointments(Authentication authentication) {
        return ResponseEntity.ok(appointmentService.listAppointments(authentication.getName()));
    }
}
