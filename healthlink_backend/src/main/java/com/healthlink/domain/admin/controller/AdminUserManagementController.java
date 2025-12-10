package com.healthlink.domain.admin.controller;

import com.healthlink.domain.user.dto.UserDto;
import com.healthlink.domain.user.dto.UserProfileResponse;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Admin endpoints for managing all users")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserManagementController {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @GetMapping
    @Operation(summary = "Get all users, optionally filtered by role")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers(@RequestParam(required = false) String role) {
        List<User> users;
        
        if (role != null && !role.isEmpty()) {
            try {
                UserRole userRole = UserRole.valueOf(role.toUpperCase());
                users = userRepository.findByRole(userRole).stream()
                        .filter(user -> user.getDeletedAt() == null)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                users = userRepository.findAll().stream()
                        .filter(user -> user.getDeletedAt() == null)
                        .collect(Collectors.toList());
            }
        } else {
            users = userRepository.findAll().stream()
                    .filter(user -> user.getDeletedAt() == null)
                    .collect(Collectors.toList());
        }
        
        List<UserProfileResponse> userDtos = users.stream()
                .map(this::toProfile)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(userDtos);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getDeletedAt() != null) {
            throw new RuntimeException("User not found");
        }
        
        return ResponseEntity.ok(toProfile(user));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete (soft delete) a user")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.softDelete();
        userRepository.save(user);
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/approve")
    @Operation(summary = "Approve a user (set approval status to APPROVED and isActive to true)")
    public ResponseEntity<Void> approveUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getDeletedAt() != null) {
            throw new RuntimeException("User not found");
        }
        
        // Set approval status to APPROVED if it exists
        if (user.getApprovalStatus() != null) {
            user.setApprovalStatus(ApprovalStatus.APPROVED);
        }
        
        // Set user as active and verified
        user.setIsActive(true);
        user.setIsEmailVerified(true);
        
        userRepository.save(user);
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspend a user (set isActive to false)")
    public ResponseEntity<Void> suspendUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getDeletedAt() != null) {
            throw new RuntimeException("User not found");
        }
        
        // Set user as inactive
        user.setIsActive(false);
        
        userRepository.save(user);
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/unsuspend")
    @Operation(summary = "Unsuspend a user (set isActive to true)")
    public ResponseEntity<Void> unsuspendUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getDeletedAt() != null) {
            throw new RuntimeException("User not found");
        }

        user.setIsActive(true);
        userRepository.save(user);

        return ResponseEntity.noContent().build();
    }

    private UserProfileResponse toProfile(User user) {
        String specialization = null;
        String pmdcId = null;
        Integer yearsOfExperience = null;

        if (user instanceof Doctor doctor) {
            specialization = doctor.getSpecialization();
            pmdcId = doctor.getPmdcId();
            yearsOfExperience = doctor.getYearsOfExperience();
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .isEmailVerified(Boolean.TRUE.equals(user.getIsEmailVerified()))
                .isActive(Boolean.TRUE.equals(user.getIsActive()))
                .approvalStatus(user.getApprovalStatus())
                .specialization(specialization)
                .pmdcId(pmdcId)
                .yearsOfExperience(yearsOfExperience)
                .createdAt(user.getCreatedAt())
                .build();
    }
}

