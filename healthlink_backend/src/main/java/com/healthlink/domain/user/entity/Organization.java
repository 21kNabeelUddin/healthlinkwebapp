package com.healthlink.domain.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Organization entity
 * Requires valid 7-digit Pakistan Organization Number for registration
 * Approval required by Admin before activation
 */
@Entity
@DiscriminatorValue("ORGANIZATION")
@Getter
@Setter
@NoArgsConstructor
public class Organization extends User {

    @NotBlank(message = "Organization name is required")
    @Size(min = 3, max = 300, message = "Organization name must be between 3 and 300 characters")
    @Column(name = "organization_name", nullable = true, length = 300)
    private String organizationName;

    @NotBlank(message = "Pakistan Organization Number is required")
    @Pattern(regexp = "^\\d{7}$", message = "Pakistan Organization Number must be exactly 7 digits")
    @Column(name = "pakistan_org_number", nullable = true, unique = true, length = 7)
    private String pakistanOrgNumber; // 7-digit number validated by Admin

    @Column(name = "org_verified")
    private Boolean orgVerified = false;

    @Column(name = "registration_document_url", length = 500)
    private String registrationDocumentUrl;

    @Column(name = "organization_type", length = 100)
    private String organizationType; // HOSPITAL, CLINIC, DIAGNOSTIC_CENTER, PHARMACY

    @Column(name = "headquarters_address", length = 500)
    private String headquartersAddress;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "website_url", length = 300)
    private String websiteUrl;

    @Column(name = "total_doctors")
    private Integer totalDoctors = 0;

    @Column(name = "total_staff")
    private Integer totalStaff = 0;

    @Column(name = "total_facilities")
    private Integer totalFacilities = 0;

    // Payment account configuration mode
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Payment account mode is required")
    @Column(name = "payment_account_mode", nullable = true, length = 50)
    private PaymentAccountMode paymentAccountMode = PaymentAccountMode.DOCTOR_LEVEL;

    /**
     * Validate Pakistan Organization Number format
     */
    @PrePersist
    @PreUpdate
    private void validatePakistanOrgNumber() {
        if (pakistanOrgNumber != null && !pakistanOrgNumber.matches("^\\d{7}$")) {
            throw new IllegalArgumentException(
                    "Pakistan Organization Number must be exactly 7 digits. Provided: " + pakistanOrgNumber);
        }
    }
}
