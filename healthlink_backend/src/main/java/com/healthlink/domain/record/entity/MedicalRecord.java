package com.healthlink.domain.record.entity;

import com.healthlink.common.entity.BaseEntity;
import com.healthlink.security.encryption.FieldEncryptionConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

/**
 * MedicalRecord
 * Unified entity representing a patient medical record entry supporting both:
 *  - Structured encrypted PHI text (details, description)
 *  - Optional file/attachment references (attachmentUrl, fileUrl)
 *  - Simple doctor association via doctorId
 * HIPAA: All PHI-bearing free text fields use {@link FieldEncryptionConverter}.
 */
@Entity
@Table(name = "medical_records", indexes = {
        @Index(name = "idx_medrec_patient", columnList = "patient_id"),
        @Index(name = "idx_medrec_created", columnList = "created_at")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MedicalRecord extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "doctor_id")
    private UUID doctorId;

    @Column(name = "record_type", length = 60)
    private String recordType;

    @Column(name = "title", length = 180, nullable = false)
    private String title;

    @Column(name = "summary", length = 500)
    private String summary;

    @Lob
    @Convert(converter = FieldEncryptionConverter.class)
    @Column(name = "details", nullable = false)
    private String details;

    @Convert(converter = FieldEncryptionConverter.class)
    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "attachment_url", length = 512)
    private String attachmentUrl;

    @Column(name = "file_url", length = 512)
    private String fileUrl;

    // createdAt / updatedAt provided by BaseEntity auditing (LocalDateTime)
}
