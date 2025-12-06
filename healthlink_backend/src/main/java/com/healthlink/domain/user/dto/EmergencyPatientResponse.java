package com.healthlink.domain.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class EmergencyPatientResponse {
    private UUID patientId;
    private String email;
    private String patientName;
    private String phoneNumber;
}

