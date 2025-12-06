package com.healthlink.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateEmergencyPatientRequest {
    @NotBlank(message = "Patient name is required")
    private String patientName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    private String phoneNumber; // Optional
}

