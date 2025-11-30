package com.healthlink.domain.appointment.repository;

import com.healthlink.domain.appointment.entity.PaymentDispute;
import com.healthlink.domain.appointment.entity.PaymentDisputeStage;
import com.healthlink.domain.appointment.entity.PaymentDisputeResolution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentDisputeRepository extends JpaRepository<PaymentDispute, UUID> {
    List<PaymentDispute> findByStageOrderByCreatedAtAsc(PaymentDisputeStage stage);
    List<PaymentDispute> findByResolutionStatus(PaymentDisputeResolution resolutionStatus);
}
