package com.healthlink.service.payment;

import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Organization;
import com.healthlink.domain.user.entity.PaymentAccountMode;
import com.healthlink.domain.user.repository.DoctorRepository;
import com.healthlink.domain.user.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Payment Account Resolver
 * Determines which payment account to use based on organization's payment mode
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAccountResolver {

    private final DoctorRepository doctorRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Resolve payment account details for a doctor's appointment
     * Based on organization's payment account mode configuration
     */
    public PaymentAccountDetails resolvePaymentAccount(UUID doctorId, UUID organizationId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        // Get organization to check payment mode
        Organization organization = null;
        if (organizationId != null) {
            organization = organizationRepository.findById(organizationId)
                    .orElse(null);
        }

        // Determine payment mode
        PaymentAccountMode mode = PaymentAccountMode.DOCTOR_LEVEL; // Default
        if (organization != null) {
            mode = organization.getPaymentAccountMode();
        }

        PaymentAccountDetails details = new PaymentAccountDetails();
        details.setMode(mode);
        details.setDoctorId(doctorId);
        details.setOrganizationId(organizationId);

        switch (mode) {
            case DOCTOR_LEVEL:
                // Payment goes directly to doctor's account
                details.setAccountHolderType("DOCTOR");
                details.setAccountHolderId(doctorId);
                details.setDescription("Payment to Dr. " + doctor.getFullName());
                log.debug("Resolved payment to doctor-level account for doctor: {}", doctorId);
                break;

            case CENTRALIZED_ORG:
                // Payment goes to organization's central account
                if (organization == null) {
                    throw new IllegalStateException(
                            "Organization required for centralized payment mode but not found");
                }
                details.setAccountHolderType("ORGANIZATION");
                details.setAccountHolderId(organizationId);
                details.setDescription("Payment to " + organization.getOrganizationName() + " (centralized)");
                log.debug("Resolved payment to centralized org account for organization: {}", organizationId);
                break;

            default:
                throw new IllegalStateException("Unsupported payment account mode: " + mode);
        }

        return details;
    }

    /**
     * Verify if account is properly configured for payments
     */
    public boolean isAccountConfigured(UUID doctorId, UUID organizationId) {
        try {
            PaymentAccountDetails details = resolvePaymentAccount(doctorId, organizationId);
            // TODO: Add checks for actual bank account/payment gateway configuration
            return details != null && details.getAccountHolderId() != null;
        } catch (Exception e) {
            log.error("Failed to verify payment account configuration", e);
            return false;
        }
    }

    /**
     * Payment Account Details DTO
     */
    @lombok.Data
    public static class PaymentAccountDetails {
        private PaymentAccountMode mode;
        private UUID doctorId;
        private UUID organizationId;
        private String accountHolderType; // "DOCTOR" or "ORGANIZATION"
        private UUID accountHolderId;
        private String description;

        // Future: Add actual payment gateway account identifiers
        // private String stripeAccountId;
        // private String easyPaisaAccountNumber;
        // private String jazzCashAccountNumber;
    }
}
