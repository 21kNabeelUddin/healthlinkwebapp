package com.healthlink.domain.user.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PmdcLicenseValidator
 */
@ExtendWith(MockitoExtension.class)
class PmdcLicenseValidatorTest {

    @InjectMocks
    private PmdcLicenseValidator validator;

    @Test
    void shouldValidateFullFormatMedicalLicense() {
        // Given
        String license = "M-12345-N";
        
        // When
        boolean isValid = validator.isValidFormat(license);
        
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldValidateFullFormatDentalLicense() {
        // Given
        String license = "D-54321-P";
        
        // When
        boolean isValid = validator.isValidFormat(license);
        
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldValidateSimpleFormatLicense() {
        // Given
        String license = "12345";
        
        // When
        boolean isValid = validator.isValidFormat(license);
        
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldValidateSixDigitSimpleFormat() {
        // Given
        String license = "123456";
        
        // When
        boolean isValid = validator.isValidFormat(license);
        
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectInvalidFormat() {
        // Given
        String license = "ABC-12345";
        
        // When
        boolean isValid = validator.isValidFormat(license);
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectNullLicense() {
        // Given
        String license = null;
        
        // When
        boolean isValid = validator.isValidFormat(license);
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectBlankLicense() {
        // Given
        String license = "   ";
        
        // When
        boolean isValid = validator.isValidFormat(license);
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldExtractMedicalLicenseType() {
        // Given
        String license = "M-12345-N";
        
        // When
        String type = validator.getLicenseType(license);
        
        // Then
        assertThat(type).isEqualTo("MEDICAL");
    }

    @Test
    void shouldExtractDentalLicenseType() {
        // Given
        String license = "D-12345-P";
        
        // When
        String type = validator.getLicenseType(license);
        
        // Then
        assertThat(type).isEqualTo("DENTAL");
    }

    @Test
    void shouldReturnUnknownForSimpleFormat() {
        // Given
        String license = "12345";
        
        // When
        String type = validator.getLicenseType(license);
        
        // Then
        assertThat(type).isEqualTo("UNKNOWN");
    }

    @Test
    void shouldExtractNationalScope() {
        // Given
        String license = "M-12345-N";
        
        // When
        String scope = validator.getRegistrationScope(license);
        
        // Then
        assertThat(scope).isEqualTo("NATIONAL");
    }

    @Test
    void shouldExtractProvincialScope() {
        // Given
        String license = "D-54321-P";
        
        // When
        String scope = validator.getRegistrationScope(license);
        
        // Then
        assertThat(scope).isEqualTo("PROVINCIAL");
    }

    @Test
    void shouldNormalizeLicenseNumber() {
        // Given
        String license = "  m-12345-n  ";
        
        // When
        String normalized = validator.normalize(license);
        
        // Then
        assertThat(normalized).isEqualTo("M-12345-N");
    }

    @Test
    void shouldValidateWithDetailedResult() {
        // Given
        String license = "M-12345-N";
        
        // When
        PmdcLicenseValidator.ValidationResult result = validator.validate(license);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getLicenseType()).isEqualTo("MEDICAL");
        assertThat(result.getRegistrationScope()).isEqualTo("NATIONAL");
        assertThat(result.getNormalizedNumber()).isEqualTo("M-12345-N");
    }

    @Test
    void shouldReturnDetailedErrorForInvalidLicense() {
        // Given
        String license = "INVALID";
        
        // When
        PmdcLicenseValidator.ValidationResult result = validator.validate(license);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).contains("Invalid PMDC license format");
    }

    @Test
    void shouldHandleLowercaseInput() {
        // Given
        String license = "m-12345-n";
        
        // When
        boolean isValid = validator.isValidFormat(license);
        
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldHandleSpacesInLicense() {
        // Given
        String license = "M - 12345 - N";
        
        // When
        String normalized = validator.normalize(license);
        boolean isValid = validator.isValidFormat(normalized);
        
        // Then
        assertThat(normalized).isEqualTo("M-12345-N");
        assertThat(isValid).isTrue();
    }
}
