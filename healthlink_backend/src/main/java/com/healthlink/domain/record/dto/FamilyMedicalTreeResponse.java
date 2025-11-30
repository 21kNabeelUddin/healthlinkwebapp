package com.healthlink.domain.record.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMedicalTreeResponse {
    private UUID id;
    private UUID patientId;
    private String relativeName;
    private String relationship; // Also mapped as "relation" for mobile compatibility
    private String condition;
    private LocalDateTime diagnosedAt;
    private String notes;
    private Boolean deceased;
    private Boolean consented; // Added for mobile compatibility
    private Integer ageAtDiagnosis;
    private List<FamilyMedicalTreeResponse> children; // Recursive structure for tree
}
