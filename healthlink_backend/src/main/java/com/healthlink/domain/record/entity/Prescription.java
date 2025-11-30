package com.healthlink.domain.record.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import com.healthlink.security.encryption.FieldEncryptionConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prescriptions", indexes = {
        @Index(name = "idx_prescription_patient", columnList = "patient_id"),
        @Index(name = "idx_prescription_appointment", columnList = "appointment_id"),
        @Index(name = "idx_prescription_doctor", columnList = "doctor_id")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Prescription extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Column(name = "doctor_id")
    private UUID doctorId;

    @Column(name = "title", length = 180, nullable = false)
    private String title;

    @Lob
    @Convert(converter = FieldEncryptionConverter.class)
    @Column(name = "body", nullable = false)
    private String body; // Encrypted PHI prescription details

    // Stored as element collection for query flexibility; for existing schema with
    // TEXT column this may require migration
    @ElementCollection
    @CollectionTable(name = "prescription_medications", joinColumns = @JoinColumn(name = "prescription_id"))
    @Column(name = "medication", length = 160)
    private List<String> medications;

    @ElementCollection
    @CollectionTable(name = "prescription_warnings", joinColumns = @JoinColumn(name = "prescription_id"))
    @Column(name = "warning", length = 300)
    private List<String> interactionWarnings;

    // e-signature removed per spec
    // createdAt provided by BaseEntity auditing (LocalDateTime)

    // createdAt provided by BaseEntity auditing (LocalDateTime)
}
