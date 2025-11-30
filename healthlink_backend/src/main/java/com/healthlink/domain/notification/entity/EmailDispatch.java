package com.healthlink.domain.notification.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_dispatches", indexes = {
        @Index(name = "idx_email_dispatch_user", columnList = "user_id"),
        @Index(name = "idx_email_dispatch_type", columnList = "email_type")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EmailDispatch extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "email_type", length = 60, nullable = false)
    private String emailType; // APPROVAL_DECISION, ORG_APPROVAL_DECISION

    @Column(name = "status", length = 20, nullable = false)
    private String status; // SENT / FAILED

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "attempted_at", nullable = false)
    private OffsetDateTime attemptedAt = OffsetDateTime.now();
}
