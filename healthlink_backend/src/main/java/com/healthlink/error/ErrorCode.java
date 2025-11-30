package com.healthlink.error;

/**
 * Standardized application error codes grouped by domain.
 */
public enum ErrorCode {
    // Auth & Security
    AUTH_INVALID_CREDENTIALS,
    AUTH_ACCOUNT_INACTIVE,
    AUTH_ACCOUNT_PENDING,
    AUTH_ACCOUNT_REJECTED,
    AUTH_EMAIL_UNVERIFIED,
    AUTH_PASSWORD_REQUIRED,
    AUTH_OTP_REQUIRED,
    AUTH_OTP_INVALID,
    AUTH_TOKEN_INVALID,
    AUTH_FORBIDDEN,

    // Validation / Input
    VALIDATION_FAILED,
    RESOURCE_NOT_FOUND,
    CONFLICT,

    // PHI / Privacy
    PHI_ACCESS_DENIED,

    // Export / Admin
    EXPORT_ALREADY_RUNNING,
    EXPORT_NOT_FOUND,

    // System / Unknown
    SYSTEM_ERROR;
}
