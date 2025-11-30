package com.healthlink.domain.record.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import com.healthlink.security.encryption.FieldEncryptionConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a family medical history entry for a patient.
 * Stored separately to allow querying and future analytics.
 */
@Entity
@Table(name = "family_medical_tree")
@Getter
@Setter
@NoArgsConstructor
public class FamilyMedicalTree extends BaseEntity {
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "relative_name", length = 200, nullable = false)
    private String relativeName; // e.g., "Father", "Mother"

    @Column(name = "relationship", length = 100, nullable = false)
    private String relationship; // e.g., "parent", "sibling"

    @Column(name = "condition", length = 300, nullable = false)
    private String condition; // e.g., "Hypertension"

    @Column(name = "diagnosed_at")
    private LocalDateTime diagnosedAt;

    @Convert(converter = FieldEncryptionConverter.class)
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Encrypted PHI notes

    @Column(name = "deceased")
    private Boolean deceased;

    @Column(name = "age_at_diagnosis")
    private Integer ageAtDiagnosis;
}
