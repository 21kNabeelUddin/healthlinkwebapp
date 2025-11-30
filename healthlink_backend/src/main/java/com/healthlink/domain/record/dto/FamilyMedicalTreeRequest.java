package com.healthlink.domain.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for family medical history operations.
 * Matches mobile FamilyMember entity structure:
 * - relativeName → name (mobile)
 * - condition → conditions (mobile splits CSV)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMedicalTreeRequest {
    
    @NotBlank(message = "{validation.family.name.required}")
    private String relativeName;
    
    @NotNull(message = "{validation.family.relationship.required}")
    private String relationship; // FATHER, MOTHER, SIBLING, GRANDPARENT, CHILD
    
    private String condition; // Comma-separated conditions
    
    private LocalDateTime diagnosedAt;
    
    private String notes;
    
    private Boolean deceased;
    
    private Integer ageAtDiagnosis;
}
