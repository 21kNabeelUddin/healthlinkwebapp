package com.healthlink.domain.appointment.entity;

public enum PaymentStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    REJECTED,
    AUTHORIZED, // external provider authorized but not captured
    CAPTURED,    // funds captured
    REFUND_REQUESTED,
    REFUNDED,
    FAILED
}
