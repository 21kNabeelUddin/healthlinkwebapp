package com.healthlink.analytics;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "analytics_events", indexes = {
        @Index(name = "idx_ae_type", columnList = "type"),
        @Index(name = "idx_ae_occurred", columnList = "occurred_at"),
        @Index(name = "idx_ae_actor", columnList = "actor")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyticsEvent {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String type; // enum name

    @Column(name = "actor", length = 120)
    private String actor; // username or system

    @Column(name = "subject_id", length = 120)
    private String subjectId; // e.g., record/prescription id

    @Column(name = "meta", length = 1000)
    private String meta; // small JSON/kv string (non-PHI)

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @PrePersist
    void pre() { if (occurredAt == null) occurredAt = OffsetDateTime.now(); }
}
