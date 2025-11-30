package com.healthlink.domain.appointment.entity;

public enum PaymentDisputeResolution {
    OPEN,
    PATIENT_FAVORED,
    PRACTICE_FAVORED,
    ADMIN_PENDING,
    CLOSED,
    UPHELD // Restored to satisfy existing tests expecting UPHELD
}
