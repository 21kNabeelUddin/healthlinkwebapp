package com.healthlink.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    private String password;  // Optional for OTP-based login
    
    private String otp;  // Optional for OTP-based login
    
    @Builder.Default
    private Boolean rememberMe = false;
}
