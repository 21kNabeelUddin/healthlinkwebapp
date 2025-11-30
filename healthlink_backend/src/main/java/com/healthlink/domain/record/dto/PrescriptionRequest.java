package com.healthlink.domain.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PrescriptionRequest {
    private UUID appointmentId;
    private UUID patientId;
    @NotBlank
    @Size(max = 180)
    private String title;
    @NotBlank
    private String body; // raw prescription instructions
    private List<String> medications; // list of medication names for interaction check
    private UUID templateId; // optional template reference
}
