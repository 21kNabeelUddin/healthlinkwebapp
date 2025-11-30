package com.healthlink.domain.appointment.dto;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class PaymentDisputeHistoryEntry {
    UUID id;
    UUID disputeId;
    String fromStage;
    String toStage;
    UUID changedByUserId;
    String note;
    String resolutionStatus;
    OffsetDateTime createdAt;
}