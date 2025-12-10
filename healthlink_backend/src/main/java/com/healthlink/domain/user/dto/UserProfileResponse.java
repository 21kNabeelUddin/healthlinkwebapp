package com.healthlink.domain.user.dto;

import com.healthlink.domain.user.enums.ApprovalStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role;
    private boolean isEmailVerified;
    private boolean isActive;
    private ApprovalStatus approvalStatus;
    private String specialization;
    private String pmdcId;
    private Integer yearsOfExperience;
    private LocalDateTime createdAt;
}

