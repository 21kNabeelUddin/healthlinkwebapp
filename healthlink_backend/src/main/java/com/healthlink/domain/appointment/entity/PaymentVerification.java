package com.healthlink.domain.appointment.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_verifications", indexes = {
        @Index(name = "idx_payverify_payment", columnList = "payment_id"),
        @Index(name = "idx_payverify_status", columnList = "status")
})
@Getter
@Setter
public class PaymentVerification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "verifier_user_id", nullable = false)
    private UUID verifierUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentVerificationStatus status;

    @Column(name = "notes", length = 512)
    private String notes;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "disputed", nullable = false)
    private boolean disputed = false;
}
