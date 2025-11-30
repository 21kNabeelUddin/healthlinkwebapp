package com.healthlink.domain.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Doctor entity
 * Requires valid PMDC (Pakistan Medical and Dental Council) ID
 * Format: xxxxx-P (5 digits followed by dash and P)
 * Admin/Organization approval required before activation
 */
@Entity
@DiscriminatorValue("DOCTOR")
@Getter
@Setter
@NoArgsConstructor
public class Doctor extends User {

    // Validation annotations enforce non-null at Java level for Doctor instances.
    // Column marked nullable = true for Single Table Inheritance compatibility
    // (Non-Doctor users don't have pmdc_id, so DB column must allow NULL)
    @NotBlank(message = "PMDC ID is required")
    @Pattern(regexp = "^\\d{5}-P$", message = "PMDC ID must be in format: xxxxx-P (5 digits, dash, uppercase P)")
    @Column(name = "pmdc_id", nullable = true, unique = true, length = 8)
    private String pmdcId; // Format: 12345-P

    @Column(name = "pmdc_verified", nullable = true, columnDefinition = "boolean default false")
    private Boolean pmdcVerified = false;

    @Column(name = "license_document_url", length = 500)
    private String licenseDocumentUrl;

    // Validation annotations enforce non-null at Java level for Doctor instances.
    // Column marked nullable = true for Single Table Inheritance compatibility
    @NotBlank(message = "Specialization is required")
    @Column(name = "specialization", nullable = true, length = 200)
    private String specialization;

    @ElementCollection
    @CollectionTable(name = "doctor_qualifications", joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "qualification")
    private Set<String> qualifications = new HashSet<>();

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "consultation_fee")
    private java.math.BigDecimal consultationFee;

    @Column(name = "bio", length = 2000)
    private String bio;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    // Practice settings
    @Column(name = "allow_early_checkin")
    private Boolean allowEarlyCheckin = false;

    @Column(name = "early_checkin_minutes")
    private Integer earlyCheckinMinutes = 15;

    @Column(name = "slot_duration_minutes")
    private Integer slotDurationMinutes = 15;

    // Refund Policy
    @Column(name = "refund_cutoff_minutes")
    private Integer refundCutoffMinutes = 1440; // 24 hours

    @Column(name = "refund_deduction_percent")
    private Double refundDeductionPercent = 0.0;

    @Column(name = "allow_full_refund_on_doctor_cancellation", nullable = false, columnDefinition = "boolean default true")
    private Boolean allowFullRefundOnDoctorCancellation = true;

    /**
     * Validate PMDC ID format on persist/update
     */
    @PrePersist
    @PreUpdate
    private void validatePmdcId() {
        if (pmdcId != null && !pmdcId.matches("^\\d{5}-P$")) {
            throw new IllegalArgumentException(
                    "PMDC ID must be in format: xxxxx-P (5 digits-P). Provided: " + pmdcId);
        }
    }

    /**
     * Check if doctor can accept appointments
     */
    public boolean canAcceptAppointments() {
        return pmdcVerified && canLogin();
    }
}
