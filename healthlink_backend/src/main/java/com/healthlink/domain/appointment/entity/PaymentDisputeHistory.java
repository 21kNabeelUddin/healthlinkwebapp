package com.healthlink.domain.appointment.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "payment_dispute_history", indexes = {
        @Index(name = "idx_history_dispute", columnList = "dispute_id")
})
@Getter
@Setter
public class PaymentDisputeHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private PaymentDispute dispute;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage")
    private PaymentDisputeStage fromStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage")
    private PaymentDisputeStage toStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_status")
    private PaymentDisputeResolution resolutionStatus;

    @Column(name = "changed_by_user_id", nullable = false)
    private UUID changedByUserId;

    @Column(name = "note", length = 1000)
    private String note;
}