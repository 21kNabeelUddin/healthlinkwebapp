package com.healthlink.domain.appointment.repository;

import com.healthlink.domain.appointment.entity.PaymentVerification;
import com.healthlink.domain.appointment.entity.PaymentVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentVerificationRepository extends JpaRepository<PaymentVerification, UUID> {
    List<PaymentVerification> findByStatusOrderByCreatedAtAsc(PaymentVerificationStatus status);
    List<PaymentVerification> findByVerifierUserId(UUID verifierUserId);
}
