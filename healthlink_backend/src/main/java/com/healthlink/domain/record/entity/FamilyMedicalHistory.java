package com.healthlink.domain.record.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Family Medical History Entity
 * Tracks hereditary conditions and family health patterns
 */
@Entity
@Table(name = "family_medical_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMedicalHistory extends BaseEntity {

    @NotNull
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @NotNull
    @Column(name = "relation", nullable = false, length = 50)
    private String relation; // FATHER, MOTHER, SIBLING, GRANDPARENT, etc.

    @ElementCollection
    @CollectionTable(name = "family_medical_conditions", joinColumns = @JoinColumn(name = "family_history_id"))
    @Column(name = "condition")
    @Builder.Default
    private List<String> medicalConditions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "family_chronic_diseases", joinColumns = @JoinColumn(name = "family_history_id"))
    @Column(name = "disease")
    @Builder.Default
    private List<String> chronicDiseases = new ArrayList<>();

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "age")
    private Integer age;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "has_hereditary_risk", nullable = false)
    @Builder.Default
    private Boolean hasHereditaryRisk = false;

    /**
     * Calculate hereditary risk based on conditions and family patterns
     */
    public void calculateHereditaryRisk() {
        // Simple heuristic: if any chronic disease or specific conditions exist, flag
        // as risk
        List<String> highRiskConditions = List.of(
                "diabetes", "heart disease", "cancer", "hypertension",
                "stroke", "alzheimer", "parkinson");

        boolean hasRisk = false;

        for (String condition : medicalConditions) {
            if (highRiskConditions.stream().anyMatch(risk -> condition.toLowerCase().contains(risk.toLowerCase()))) {
                hasRisk = true;
                break;
            }
        }

        if (!hasRisk) {
            for (String disease : chronicDiseases) {
                if (highRiskConditions.stream().anyMatch(risk -> disease.toLowerCase().contains(risk.toLowerCase()))) {
                    hasRisk = true;
                    break;
                }
            }
        }

        this.hasHereditaryRisk = hasRisk;
    }
}
