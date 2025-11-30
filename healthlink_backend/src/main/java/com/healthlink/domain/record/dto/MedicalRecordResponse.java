package com.healthlink.domain.record.dto;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * MedicalRecordResponse
 * DTO returned to clients. Only exposed after RBAC + PHI audit checks.
 * PHI fields (details) are decrypted server-side prior to mapping.
 */
@Value
@Builder
public class MedicalRecordResponse {
    UUID id;
    UUID patientId;
    String title;
    String summary;
    String details;
    String attachmentUrl;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
