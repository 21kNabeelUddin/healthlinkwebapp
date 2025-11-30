package com.healthlink.domain.user.entity;

/**
 * Payment Account Mode for Organizations
 */
public enum PaymentAccountMode {
    DOCTOR_LEVEL, // Payments go directly to individual doctors
    CENTRALIZED_ORG, // Payments go to organization's central account
    CENTRALIZED,
    /**
     * Legacy name used in older DB rows. Kept for compatibility and treated
     * as equivalent to `CENTRALIZED_ORG`.
     */
    ORGANIZATION_LEVEL;

    /**
     * Return the canonical/current enum equivalent for legacy values.
     */
    public PaymentAccountMode getCanonical() {
        if (this == ORGANIZATION_LEVEL) return CENTRALIZED_ORG;
        return this;
    }
}