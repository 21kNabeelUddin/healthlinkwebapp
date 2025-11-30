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
 * Simple Lab Order entity representing a lab test ordered for a patient.
 * Stored as a separate table to allow future extensions.
 */
@Entity
@Table(name = "lab_orders")
@Getter
@Setter
@NoArgsConstructor
public class LabOrder extends BaseEntity {
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "order_name", nullable = false, length = 200)
    private String orderName;

    @Convert(converter = FieldEncryptionConverter.class)
    @Column(name = "description", length = 1000)
    private String description; // Encrypted PHI lab order description

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt = LocalDateTime.now();

    @Column(name = "result_url", length = 500)
    private String resultUrl; // URL to uploaded result file (MinIO)
}
