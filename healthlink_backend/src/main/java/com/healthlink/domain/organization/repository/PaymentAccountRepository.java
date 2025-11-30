package com.healthlink.domain.organization.repository;

import com.healthlink.domain.organization.entity.PaymentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, UUID> {
    Optional<PaymentAccount> findByDoctorId(UUID doctorId);
    Optional<PaymentAccount> findByOrganizationIdAndDoctorId(UUID organizationId, UUID doctorId);
}