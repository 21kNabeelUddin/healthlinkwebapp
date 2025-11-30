package com.healthlink.domain.appointment.dto;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class PaymentDisputeResponse {
    UUID id;
    UUID verificationId;
    String stage;
    String resolutionStatus;
    UUID raisedByUserId;
    String notes;
    OffsetDateTime resolvedAt;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
