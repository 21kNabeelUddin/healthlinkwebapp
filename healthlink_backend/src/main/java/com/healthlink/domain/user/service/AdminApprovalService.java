package com.healthlink.domain.user.service;

import com.healthlink.domain.user.dto.ApprovalDecisionRequest;
import com.healthlink.domain.user.dto.PendingApprovalDto;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Organization;
import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.DoctorRepository;
import com.healthlink.domain.user.repository.OrganizationRepository;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.exception.ResourceNotFoundException;
import com.healthlink.service.auth.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Admin to manage approval workflow for doctors and organizations.
 * Sends email notifications on approval/rejection per spec requirements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminApprovalService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final OrganizationRepository organizationRepository;
    private final AuthenticationService authenticationService;
    private final com.healthlink.service.notification.EmailService emailService;

    /**
     * Get all pending approval requests (doctors and organizations).
     */
    public List<PendingApprovalDto> getPendingApprovals() {
        List<User> pendingUsers = userRepository.findByApprovalStatus(ApprovalStatus.PENDING);

        return pendingUsers.stream()
                .map(this::mapToPendingApprovalDto)
                .collect(Collectors.toList());
    }

    /**
     * Get pending approvals for a specific role (DOCTOR or ORGANIZATION).
     */
    public List<PendingApprovalDto> getPendingApprovalsByRole(UserRole role) {
        List<User> pendingUsers = userRepository.findByApprovalStatusAndRole(ApprovalStatus.PENDING, role);

        return pendingUsers.stream()
                .map(this::mapToPendingApprovalDto)
                .collect(Collectors.toList());
    }

    /**
     * Approve or reject a user account.
     * Sends email notification to user's registered email.
     */
    @Transactional
    public void processApprovalDecision(UUID userId, ApprovalDecisionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("User is not in PENDING status");
        }

        if (request.getStatus() == ApprovalStatus.REJECTED &&
                (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {
            throw new IllegalArgumentException("Rejection reason is required when rejecting");
        }
        // Update approval status
        authenticationService.updateApprovalStatus(userId, request.getStatus());

        // Send email notification
        if (request.getStatus() == ApprovalStatus.APPROVED) {
            emailService.sendAccountApprovalEmail(user.getEmail(), user.getFullName(), user.getRole().name());
        } else {
            emailService.sendAccountRejectionEmail(user.getEmail(), user.getFullName(), user.getRole().name(),
                    request.getRejectionReason());
        }

        log.info("Admin processed approval for user {} with status {}", userId, request.getStatus());
    }

    private PendingApprovalDto mapToPendingApprovalDto(User user) {
        PendingApprovalDto.PendingApprovalDtoBuilder builder = PendingApprovalDto.builder()
                .userId(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .registeredAt(
                        user.getCreatedAt() != null ? user.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : null)
                .currentStatus(user.getApprovalStatus());

        // Add role-specific data
        if (user.getRole() == UserRole.DOCTOR) {
            Doctor doctor = doctorRepository.findById(user.getId()).orElse(null);
            if (doctor != null) {
                builder.pmdcId(doctor.getPmdcId())
                        .specialization(doctor.getSpecialization())
                        .licenseDocumentUrl(doctor.getLicenseDocumentUrl());
            }
        } else if (user.getRole() == UserRole.ORGANIZATION) {
            Organization org = organizationRepository.findByUserId(user.getId())
                    .orElse(null);
            if (org != null) {
                builder.organizationNumber(org.getPakistanOrgNumber())
                        .organizationEmail(user.getEmail())
                        .organizationName(org.getOrganizationName());
            }
        }

        return builder.build();
    }

}
