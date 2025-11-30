package com.healthlink.domain.video.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class InitiateCallRequest {
    @NotNull(message = "Appointment ID is required")
    private UUID appointmentId;
}
