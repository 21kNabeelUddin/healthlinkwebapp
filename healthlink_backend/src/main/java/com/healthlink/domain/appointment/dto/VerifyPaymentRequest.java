package com.healthlink.domain.appointment.dto;

import com.healthlink.domain.appointment.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class VerifyPaymentRequest {
    @NotNull(message = "Payment ID is required")
    private UUID paymentId;

    @NotNull(message = "Status is required")
    private PaymentStatus status; // VERIFIED, REJECTED, AUTHORIZED, CAPTURED, FAILED

    private String verificationNotes;
}
