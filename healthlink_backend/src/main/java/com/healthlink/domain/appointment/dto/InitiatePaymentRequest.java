package com.healthlink.domain.appointment.dto;

import com.healthlink.domain.appointment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class InitiatePaymentRequest {
    @NotNull(message = "Appointment ID is required")
    private UUID appointmentId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;
}
