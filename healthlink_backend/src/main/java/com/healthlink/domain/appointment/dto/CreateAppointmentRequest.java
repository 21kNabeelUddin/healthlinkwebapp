package com.healthlink.domain.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateAppointmentRequest {
    @NotNull(message = "Doctor ID is required")
    private UUID doctorId;

    @NotNull(message = "Facility ID is required")
    private UUID facilityId;

    private UUID serviceOfferingId;

    @NotNull(message = "Appointment time is required")
    @Future(message = "Appointment time must be in the future")
    private LocalDateTime appointmentTime;

    private String reasonForVisit;
}
