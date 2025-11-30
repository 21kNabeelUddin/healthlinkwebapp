package com.healthlink.domain.user.dto;

import com.healthlink.domain.user.enums.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request to approve or reject a user account (doctor or organization).
 */
@Data
public class ApprovalDecisionRequest {
    @NotNull(message = "Approval status is required")
    private ApprovalStatus status; // APPROVED or REJECTED
    
    private String rejectionReason; // Required if status is REJECTED
}
