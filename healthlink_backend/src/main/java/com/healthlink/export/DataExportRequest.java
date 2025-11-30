package com.healthlink.export;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "data_export_requests", indexes = {
        @Index(name = "idx_export_status", columnList = "status"),
        @Index(name = "idx_export_requested", columnList = "requested_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DataExportRequest {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy; // username

    @Column(name = "status", nullable = false)
    private String status; // PENDING, RUNNING, COMPLETED, FAILED

    @Column(name = "type", nullable = false)
    private String type; // FULL or PATIENT_SCOPED or CUSTOM

    @Column(name = "patient_id")
    private UUID patientId; // optional scope

    @Column(name = "file_url", length = 600)
    private String fileUrl; // location of exported data (e.g., MinIO or local path)

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @PrePersist
    void pre() {
        if (requestedAt == null) requestedAt = OffsetDateTime.now();
        if (status == null) status = "PENDING";
    }
}
