package com.healthlink.domain.security.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_operation", columnList = "operation"),
        @Index(name = "idx_audit_target", columnList = "target_ref")
})
@Getter
@Setter
public class AuditEvent extends BaseEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "operation", length = 64, nullable = false)
    private String operation;

    @Column(name = "target_ref", length = 128)
    private String targetRef;

    @Column(name = "details", length = 512)
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt = OffsetDateTime.now();
}
