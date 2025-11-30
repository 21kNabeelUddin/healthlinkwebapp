package com.healthlink.domain.user.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Pakistan Medical & Dental Council (PMDC) License Validator
 * 
 * PMDC license format: M-12345-N (5 digits with optional prefix/suffix)
 * 
 * This validator checks:
 * 1. Format validity (regex pattern matching)
 * 2. Checksum validation (if applicable)
 * 
 * Note: Full validation requires PMDC API integration (not publicly available).
 * This implementation provides format validation only.
 * 
 * @see <a href="https://www.pmdc.org.pk">PMDC Official Website</a>
 */
@Component
@Slf4j
public class PmdcLicenseValidator {

    // PMDC license patterns:
    // Format 1: M-12345-N (Medical, 5 digits, National)
    // Format 2: D-12345-P (Dental, 5 digits, Provincial)
    // Format 3: 12345 (Simple 5-digit format)
    private static final Pattern PMDC_PATTERN_FULL = Pattern.compile("^[MD]-\\d{5}-[NP]$");
    private static final Pattern PMDC_PATTERN_SIMPLE = Pattern.compile("^\\d{5,6}$");

    /**
     * Validate PMDC license number format.
     * 
     * @param licenseNumber PMDC license number
     * @return true if format is valid
     */
    public boolean isValidFormat(String licenseNumber) {
        if (licenseNumber == null || licenseNumber.isBlank()) {
            return false;
        }

        String normalized = licenseNumber.trim().toUpperCase();
        
        boolean isValid = PMDC_PATTERN_FULL.matcher(normalized).matches() 
                       || PMDC_PATTERN_SIMPLE.matcher(normalized).matches();
        
        if (!isValid) {
            log.warn("Invalid PMDC license format: {}", licenseNumber);
        }
        
        return isValid;
    }

    /**
     * Extract license type from PMDC number.
     * 
     * @param licenseNumber PMDC license number
     * @return "MEDICAL", "DENTAL", or "UNKNOWN"
     */
    public String getLicenseType(String licenseNumber) {
        if (licenseNumber == null || licenseNumber.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = licenseNumber.trim().toUpperCase();
        
        if (normalized.startsWith("M-")) {
            return "MEDICAL";
        } else if (normalized.startsWith("D-")) {
            return "DENTAL";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Extract registration scope from PMDC number.
     * 
     * @param licenseNumber PMDC license number
     * @return "NATIONAL", "PROVINCIAL", or "UNKNOWN"
     */
    public String getRegistrationScope(String licenseNumber) {
        if (licenseNumber == null || licenseNumber.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = licenseNumber.trim().toUpperCase();
        
        if (normalized.endsWith("-N")) {
            return "NATIONAL";
        } else if (normalized.endsWith("-P")) {
            return "PROVINCIAL";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Normalize PMDC license number to standard format.
     * Removes spaces, converts to uppercase.
     * 
     * @param licenseNumber Raw PMDC license number
     * @return Normalized license number
     */
    public String normalize(String licenseNumber) {
        if (licenseNumber == null || licenseNumber.isBlank()) {
            return null;
        }
        
        return licenseNumber.trim()
                           .toUpperCase()
                           .replaceAll("\\s+", "");
    }

    /**
     * Validate PMDC license with detailed result.
     * 
     * @param licenseNumber PMDC license number
     * @return Validation result with details
     */
    public ValidationResult validate(String licenseNumber) {
        ValidationResult result = new ValidationResult();
        result.setLicenseNumber(licenseNumber);
        
        if (licenseNumber == null || licenseNumber.isBlank()) {
            result.setValid(false);
            result.setReason("License number is required");
            return result;
        }

        String normalized = normalize(licenseNumber);
        result.setNormalizedNumber(normalized);
        result.setLicenseType(getLicenseType(normalized));
        result.setRegistrationScope(getRegistrationScope(normalized));
        
        boolean formatValid = isValidFormat(normalized);
        result.setValid(formatValid);
        
        if (!formatValid) {
            result.setReason("Invalid PMDC license format. Expected: M-12345-N or D-12345-P or 12345");
        } else {
            result.setReason("Format valid. Full verification requires PMDC API integration.");
        }
        
        return result;
    }

    /**
     * Validation result DTO
     */
    @lombok.Data
    public static class ValidationResult {
        private String licenseNumber;
        private String normalizedNumber;
        private boolean valid;
        private String reason;
        private String licenseType;        // MEDICAL, DENTAL, UNKNOWN
        private String registrationScope;  // NATIONAL, PROVINCIAL, UNKNOWN
    }
}
