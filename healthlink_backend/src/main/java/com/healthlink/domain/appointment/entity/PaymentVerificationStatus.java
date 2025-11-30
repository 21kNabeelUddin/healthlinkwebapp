package com.healthlink.domain.appointment.entity;

public enum PaymentVerificationStatus {
    PENDING_QUEUE,
    VERIFIED,
    REJECTED,
    ESCALATED,
    REFUND_REQUESTED
}
