package com.healthlink.domain.appointment.dto;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class PaymentVerificationResponse {
    UUID id;
    UUID paymentId;
    UUID verifierUserId;
    String status;
    String notes;
    boolean disputed;
    OffsetDateTime verifiedAt;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
