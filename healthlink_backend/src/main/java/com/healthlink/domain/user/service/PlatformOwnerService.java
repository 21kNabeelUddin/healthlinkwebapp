package com.healthlink.domain.user.service;

import com.healthlink.domain.user.dto.AdminUserResponse;
import com.healthlink.domain.user.dto.CreateAdminRequest;
import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.entity.Admin;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.exception.ResourceNotFoundException;
import com.healthlink.security.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Platform Owner operations: admin account management, system oversight.
 * Per spec: Platform Owner manages Admin accounts, final escalation, system-wide analytics.
 */
@Service
@RequiredArgsConstructor
public class PlatformOwnerService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    @Transactional
    public AdminUserResponse createAdmin(CreateAdminRequest request) {
        // Validate username uniqueness
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        Admin admin = new Admin();
        admin.setUsername(request.getUsername());
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setEmail(request.getEmail());
        admin.setFullName(request.getFullName());
        admin.setRole(UserRole.ADMIN);
        admin.setApprovalStatus(ApprovalStatus.APPROVED); // Admins are pre-approved
        admin.setIsActive(true);
        admin.setIsEmailVerified(true);
        admin.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(admin);
        auditLogger.logAdminCreation(saved.getId(), request.getUsername());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listAdmins() {
        return userRepository.findByRole(UserRole.ADMIN).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivateAdmin(UUID adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (admin.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("User is not an admin");
        }
        admin.setIsActive(false);
        userRepository.save(admin);
        auditLogger.logAdminDeactivation(adminId);
    }

    @Transactional
    public void reactivateAdmin(UUID adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (admin.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("User is not an admin");
        }
        admin.setIsActive(true);
        userRepository.save(admin);
        auditLogger.logAdminReactivation(adminId);
    }

    private AdminUserResponse mapToResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .active(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
