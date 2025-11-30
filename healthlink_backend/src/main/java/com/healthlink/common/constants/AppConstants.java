package com.healthlink.common.constants;

/**
 * Application-wide constants
 */
public final class AppConstants {
    
    private AppConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // API Versioning
    public static final String API_VERSION = "/api/v1";
    
    // Date/Time Formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String TIMEZONE = "Asia/Karachi";
    
    // JWT
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    public static final String JWT_CLAIM_ROLE = "role";
    public static final String JWT_CLAIM_USER_ID = "userId";
    public static final String JWT_CLAIM_EMAIL = "email";
    
    // Redis Keys
    public static final String REDIS_OTP_PREFIX = "otp:";
    public static final String REDIS_REFRESH_TOKEN_PREFIX = "refresh:";
    public static final String REDIS_BLACKLIST_PREFIX = "blacklist:";
    public static final String REDIS_RATE_LIMIT_PREFIX = "ratelimit:";
    
    // PHI Constants
    public static final String PHI_MASK = "***REDACTED***";
    public static final String AUDIT_LOG_ACTION_VIEW = "VIEW";
    public static final String AUDIT_LOG_ACTION_CREATE = "CREATE";
    public static final String AUDIT_LOG_ACTION_UPDATE = "UPDATE";
    public static final String AUDIT_LOG_ACTION_DELETE = "DELETE";
    public static final String AUDIT_LOG_ACTION_EXPORT = "EXPORT";
    
    // Appointment Constants
    public static final int DEFAULT_SLOT_DURATION_MINUTES = 30;
    public static final int REMINDER_1_HOUR_MINUTES = 60;
    public static final int REMINDER_15_MINUTES = 15;
    public static final int REMINDER_5_MINUTES = 5;
    
    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    
    // File Upload
    public static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50MB
    public static final String[] ALLOWED_MEDICAL_RECORD_TYPES = {
        "application/pdf",
        "image/jpeg",
        "image/png",
        "image/tiff",
        "application/dicom"
    };
    
    // OTP
    public static final int OTP_LENGTH = 6;
    public static final int OTP_EXPIRATION_MINUTES = 5;
    public static final int MAX_OTP_ATTEMPTS = 3;
    public static final int OTP_RATE_LIMIT_PER_HOUR = 5;
    
    // Payment
    public static final String PAYMENT_STATUS_PENDING = "PENDING";
    public static final String PAYMENT_STATUS_SUCCESS = "SUCCESS";
    public static final String PAYMENT_STATUS_FAILED = "FAILED";
    public static final String PAYMENT_STATUS_REFUNDED = "REFUNDED";
    
    // Appointment Status
    public static final String APPOINTMENT_STATUS_PENDING = "PENDING";
    public static final String APPOINTMENT_STATUS_CONFIRMED = "CONFIRMED";
    public static final String APPOINTMENT_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String APPOINTMENT_STATUS_COMPLETED = "COMPLETED";
    public static final String APPOINTMENT_STATUS_CANCELED = "CANCELED";
    public static final String APPOINTMENT_STATUS_NO_SHOW = "NO_SHOW";
    
    // Roles
    public static final String ROLE_PATIENT = "PATIENT";
    public static final String ROLE_DOCTOR = "DOCTOR";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_ORGANIZATION = "ORGANIZATION";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_PLATFORM_OWNER = "PLATFORM_OWNER";
    
    // Permissions
    public static final String PERMISSION_MANAGE_PRACTICE = "MANAGE_PRACTICE";
    public static final String PERMISSION_VIEW_OWN_PHI = "VIEW_OWN_PHI";
    public static final String PERMISSION_VIEW_ASSIGNED_PATIENT_PHI = "VIEW_ASSIGNED_PATIENT_PHI";
    public static final String PERMISSION_MANAGE_APPOINTMENTS = "MANAGE_APPOINTMENTS";
    public static final String PERMISSION_APPROVE_USERS = "APPROVE_USERS";
    public static final String PERMISSION_MANAGE_ADMINS = "MANAGE_ADMINS";
    public static final String PERMISSION_INITIATE_VIDEO_CALL = "INITIATE_VIDEO_CALL";
    public static final String PERMISSION_CREATE_PRESCRIPTION = "CREATE_PRESCRIPTION";
    public static final String PERMISSION_VIEW_ANALYTICS = "VIEW_ANALYTICS";
    
    // Approval Status
    public static final String APPROVAL_STATUS_PENDING = "PENDING";
    public static final String APPROVAL_STATUS_APPROVED = "APPROVED";
    public static final String APPROVAL_STATUS_REJECTED = "REJECTED";
    
    // Validation Patterns
    public static final String PMDC_ID_PATTERN = "^[0-9]{5}-[A-Z]$"; // Example: 12345-P
    public static final String PAKISTAN_ORG_NUMBER_PATTERN = "^[0-9]{7}$";
    public static final String PHONE_PATTERN = "^(\\+92|0)?3[0-9]{9}$";
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
}
