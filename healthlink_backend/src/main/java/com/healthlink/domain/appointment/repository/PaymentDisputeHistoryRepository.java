package com.healthlink.domain.appointment.repository;

import com.healthlink.domain.appointment.entity.PaymentDisputeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentDisputeHistoryRepository extends JpaRepository<PaymentDisputeHistory, UUID> {
    List<PaymentDisputeHistory> findByDispute_IdOrderByCreatedAtAsc(UUID disputeId);
}