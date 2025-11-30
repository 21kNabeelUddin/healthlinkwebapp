package com.healthlink.domain.appointment.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_appointment", columnList = "appointment_id"),
    @Index(name = "idx_payment_status", columnList = "status")
})
@Getter
@Setter
public class Payment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "transaction_reference")
    private String transactionReference; // For manual bank transfer ref or cash receipt ID

    @Column(name = "receipt_url")
    private String receiptUrl; // URL to uploaded screenshot/receipt

    @Column(name = "currency", length = 3)
    private String currency = "PKR"; // ISO 4217 currency code

    @Column(name = "external_provider", length = 40)
    private String externalProvider; // e.g., STRIPE, JAZZCASH, EASYPAISA

    @Column(name = "external_status", length = 60)
    private String externalStatus; // provider status string for reconciliation

    @Column(name = "captured_at")
    private LocalDateTime capturedAt; // when funds captured/settled

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt; // timestamp of refund completion

    @Column(name = "attempt_count")
    private int attemptCount = 0; // number of charge/verify attempts

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "verified_by_user_id")
    private java.util.UUID verifiedByUserId; // ID of Staff or Doctor who verified

    @Column(name = "verification_notes")
    private String verificationNotes;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
