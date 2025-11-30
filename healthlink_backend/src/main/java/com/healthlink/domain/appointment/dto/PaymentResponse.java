package com.healthlink.domain.appointment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID id;
    private UUID appointmentId;
    private String patientId;
    private String doctorId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String method;
    private String transactionId;
    private String externalProvider;
    private String externalStatus;
    private Integer attemptCount;
    private java.time.LocalDateTime capturedAt;
    private java.time.LocalDateTime refundedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
