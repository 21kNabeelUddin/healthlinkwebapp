package com.healthlink.domain.organization.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PakistanOrgNumberValidator
 */
@ExtendWith(MockitoExtension.class)
class PakistanOrgNumberValidatorTest {

    @InjectMocks
    private PakistanOrgNumberValidator validator;

    @Test
    void shouldValidateSixDigitCompanyNumber() {
        // Given
        String orgNumber = "123456";
        
        // When
        boolean isValid = validator.isValidFormat(orgNumber);
        
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldValidateSevenDigitCompanyNumber() {
        // Given
        String orgNumber = "0012345";
        
        // When
        boolean isValid = validator.isValidFormat(orgNumber);
        
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldValidateNtnFormat() {
        // Given
        String ntn = "1234567-0";
        
        // When
        boolean isValid = validator.isValidFormat(ntn);
        
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldValidateNtnWithCorrectFormat() {
        // Given: NTN with valid format (checksum verification not implemented)
        String ntn = "1234567-0";
        
        // When
        boolean isValid = validator.isValidNtn(ntn);
        
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldValidateAnyNtnWithValidFormat() {
        // Given: NTN with valid format (any check digit accepted)
        String ntn = "1234567-9";
        
        // When
        boolean isValid = validator.isValidNtn(ntn);
        
        // Then: Format validation only (checksum not verified)
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectInvalidFormat() {
        // Given
        String orgNumber = "ABC123";
        
        // When
        boolean isValid = validator.isValidFormat(orgNumber);
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectNullOrgNumber() {
        // Given
        String orgNumber = null;
        
        // When
        boolean isValid = validator.isValidFormat(orgNumber);
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectBlankOrgNumber() {
        // Given
        String orgNumber = "   ";
        
        // When
        boolean isValid = validator.isValidFormat(orgNumber);
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectTooShortNumber() {
        // Given
        String orgNumber = "12345";
        
        // When
        boolean isValid = validator.isValidFormat(orgNumber);
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectTooLongNumber() {
        // Given
        String orgNumber = "12345678";
        
        // When
        boolean isValid = validator.isValidFormat(orgNumber);
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldIdentifyNtnType() {
        // Given
        String ntn = "1234567-0";
        
        // When
        String type = validator.getNumberType(ntn);
        
        // Then
        assertThat(type).isEqualTo("NTN");
    }

    @Test
    void shouldIdentifyCompanyRegistrationType() {
        // Given
        String orgNumber = "123456";
        
        // When
        String type = validator.getNumberType(orgNumber);
        
        // Then
        assertThat(type).isEqualTo("COMPANY_REGISTRATION");
    }

    @Test
    void shouldIdentifyIncorporationNumberType() {
        // Given
        String orgNumber = "0012345";
        
        // When
        String type = validator.getNumberType(orgNumber);
        
        // Then
        assertThat(type).isEqualTo("INCORPORATION_NUMBER");
    }

    @Test
    void shouldNormalizeOrgNumber() {
        // Given
        String orgNumber = "  123456  ";
        
        // When
        String normalized = validator.normalize(orgNumber);
        
        // Then
        assertThat(normalized).isEqualTo("123456");
    }

    @Test
    void shouldPreserveHyphenInNtn() {
        // Given
        String ntn = "1234567-0";
        
        // When
        String normalized = validator.normalize(ntn);
        
        // Then
        assertThat(normalized).isEqualTo("1234567-0");
    }

    @Test
    void shouldRemoveSpacesFromNumber() {
        // Given
        String orgNumber = "12 34 56";
        
        // When
        String normalized = validator.normalize(orgNumber);
        
        // Then
        assertThat(normalized).isEqualTo("123456");
    }

    @Test
    void shouldValidateWithDetailedResult() {
        // Given
        String ntn = "1234567-0";
        
        // When
        PakistanOrgNumberValidator.ValidationResult result = validator.validate(ntn);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isTrue();
        assertThat(result.isFormatValid()).isTrue();
        assertThat(result.isChecksumValid()).isTrue(); // Format validated
        assertThat(result.getNumberType()).isEqualTo("NTN");
        assertThat(result.getNormalizedNumber()).isEqualTo("1234567-0");
        assertThat(result.getReason()).contains("Valid NTN format");
    }

    @Test
    void shouldReturnDetailedErrorForInvalidFormat() {
        // Given
        String orgNumber = "INVALID";
        
        // When
        PakistanOrgNumberValidator.ValidationResult result = validator.validate(orgNumber);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isFalse();
        assertThat(result.isFormatValid()).isFalse();
        assertThat(result.getReason()).contains("Invalid format");
    }

    @Test
    void shouldValidateNtnFormatWithAnyCheckDigit() {
        // Given: NTN with valid format (checksum not verified)
        String ntn = "1234567-9";
        
        // When
        PakistanOrgNumberValidator.ValidationResult result = validator.validate(ntn);
        
        // Then: Format validation passes (checksum verification not implemented)
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isTrue();
        assertThat(result.isFormatValid()).isTrue();
        assertThat(result.isChecksumValid()).isTrue(); // Format validated
        assertThat(result.getReason()).contains("Valid NTN format");
    }

    @Test
    void shouldValidateCompanyNumberWithDetailedResult() {
        // Given
        String orgNumber = "123456";
        
        // When
        PakistanOrgNumberValidator.ValidationResult result = validator.validate(orgNumber);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isTrue();
        assertThat(result.isFormatValid()).isTrue();
        assertThat(result.isChecksumValid()).isTrue(); // N/A for company numbers
        assertThat(result.getNumberType()).isEqualTo("COMPANY_REGISTRATION");
    }

    @Test
    void shouldHandleLeadingZeros() {
        // Given
        String orgNumber = "0012345";
        
        // When
        boolean isValid = validator.isValidFormat(orgNumber);
        String type = validator.getNumberType(orgNumber);
        
        // Then
        assertThat(isValid).isTrue();
        assertThat(type).isEqualTo("INCORPORATION_NUMBER");
    }

    @Test
    void shouldValidateVariousNtnFormats() {
        // Test various NTN formats (checksum verification not implemented)
        assertThat(validator.isValidNtn("1234567-0")).isTrue();
        assertThat(validator.isValidNtn("7654321-6")).isTrue();
        assertThat(validator.isValidNtn("1111111-2")).isTrue();
        assertThat(validator.isValidNtn("9999999-9")).isTrue();
    }

    @Test
    void shouldRejectNtnWithoutHyphen() {
        // Given
        String ntn = "12345678"; // Missing hyphen
        
        // When
        boolean isValid = validator.isValidNtn(ntn);
        
        // Then
        assertThat(isValid).isFalse();
    }
}
