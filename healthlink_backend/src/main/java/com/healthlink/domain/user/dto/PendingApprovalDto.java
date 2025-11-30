package com.healthlink.domain.user.dto;

import com.healthlink.domain.user.enums.ApprovalStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for pending approval requests (doctors and organizations).
 * Used by Admin to review and approve/reject registrations.
 */
@Data
@Builder
public class PendingApprovalDto {
    private UUID userId;
    private String name;
    private String email;
    private String role; // DOCTOR or ORGANIZATION
    private OffsetDateTime registeredAt;
    private ApprovalStatus currentStatus;
    
    // Doctor-specific fields
    private String pmdcId;
    private String specialization;
    private String licenseDocumentUrl;
    
    // Organization-specific fields
    private String organizationNumber;
    private String organizationEmail;
    private String organizationName;
}
