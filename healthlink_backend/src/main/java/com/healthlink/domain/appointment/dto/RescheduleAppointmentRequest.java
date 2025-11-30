package com.healthlink.domain.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RescheduleAppointmentRequest {
    @NotNull(message = "Appointment ID is required")
    private UUID appointmentId;

    @NotNull(message = "New start time is required")
    @Future(message = "New time must be in the future")
    private LocalDateTime newStartTime;
}