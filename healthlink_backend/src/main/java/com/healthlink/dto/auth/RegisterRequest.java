package com.healthlink.dto.auth;

import com.healthlink.domain.user.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    private String password;  // Required for Admin/Platform Owner, optional for Patient (OTP-based)
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    private String phoneNumber;
    
    @NotNull(message = "Role is required")
    private UserRole role;
    
    @Builder.Default
    private String preferredLanguage = "en";
    
    // Patient-specific fields
    private String dateOfBirth;  // ISO date string (e.g., "2025-11-05T00:00:00")
    private String address;
    
    // Role-specific fields
    private String pmdcId;  // For doctors
    private String pakistanOrgNumber;  // For organizations
    private String organizationName;  // For organizations
    private String specialization;  // For doctors
    private String adminUsername;  // For admin
    private String ownerUsername;  // For platform owner
}
