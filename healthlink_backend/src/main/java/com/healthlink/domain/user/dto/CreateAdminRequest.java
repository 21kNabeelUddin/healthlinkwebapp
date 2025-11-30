package com.healthlink.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to create a new admin account.
 * Platform Owner uses username/password auth for admins (not email OTP).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "Email is required for admin communication")
    private String email;
    
    private String fullName;
}
