package com.healthlink.domain.notification;

/**
 * Types of in-app notifications (NO email, NO SMS per spec).
 * Per spec Section 11: In-app only with configurable reminders.
 */
public enum NotificationType {
    /**
     * Appointment reminder (1h, 15m, 5m before - user configurable)
     */
    APPOINTMENT_REMINDER,
    
    /**
     * Payment verification status (approved/rejected)
     */
    PAYMENT_STATUS,
    
    /**
     * Appointment confirmation (after payment verified)
     */
    APPOINTMENT_CONFIRMED,
    
    /**
     * Appointment cancellation (by patient or doctor)
     */
    APPOINTMENT_CANCELED,
    PAYMENT_VERIFIED,
    PAYMENT_REJECTED,
    PAYMENT_DISPUTED,
    VIDEO_CALL_STARTING,
    PRESCRIPTION_CREATED,
    
    /**
     * Check-in reminder (when appointment time approaches)
     */
    CHECK_IN_REMINDER,

    /**
     * Payment verification needed (for staff)
     */
    PAYMENT_VERIFICATION
}