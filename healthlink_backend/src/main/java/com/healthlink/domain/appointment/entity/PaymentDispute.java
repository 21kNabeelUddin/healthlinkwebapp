package com.healthlink.domain.appointment.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_disputes", indexes = {
        @Index(name = "idx_dispute_verification", columnList = "verification_id"),
        @Index(name = "idx_dispute_stage", columnList = "stage")
})
@Getter
@Setter
public class PaymentDispute extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id", nullable = false)
    private PaymentVerification verification;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private PaymentDisputeStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_status", nullable = false)
    private PaymentDisputeResolution resolutionStatus = PaymentDisputeResolution.OPEN;

    @Column(name = "raised_by_user_id", nullable = false)
    private UUID raisedByUserId;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
}
