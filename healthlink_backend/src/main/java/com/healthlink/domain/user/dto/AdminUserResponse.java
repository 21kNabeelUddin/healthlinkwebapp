package com.healthlink.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for admin user accounts (PHI-safe, non-sensitive).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
