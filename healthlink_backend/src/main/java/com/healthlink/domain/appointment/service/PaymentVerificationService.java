package com.healthlink.domain.appointment.service;

import com.healthlink.domain.appointment.dto.PaymentVerificationResponse;
import com.healthlink.domain.appointment.entity.Payment;
import com.healthlink.domain.appointment.entity.PaymentVerification;
import com.healthlink.domain.appointment.entity.PaymentVerificationStatus;
import com.healthlink.domain.appointment.repository.PaymentRepository;
import com.healthlink.domain.appointment.repository.PaymentVerificationRepository;
import lombok.RequiredArgsConstructor;
import com.healthlink.domain.webhook.EventType;
import com.healthlink.domain.webhook.WebhookPublisherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentVerificationService {

    private final PaymentVerificationRepository verificationRepository;
    private final PaymentRepository paymentRepository;
    private final WebhookPublisherService webhookPublisherService;

    public PaymentVerificationResponse enqueue(UUID paymentId, UUID verifierUserId, String notes) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        var pv = new PaymentVerification();
        pv.setPayment(payment);
        pv.setVerifierUserId(verifierUserId);
        pv.setStatus(PaymentVerificationStatus.PENDING_QUEUE);
        pv.setNotes(notes);
        return toDto(verificationRepository.save(pv));
    }

    public PaymentVerificationResponse verify(UUID verificationId, PaymentVerificationStatus status, String notes) {
        PaymentVerification pv = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));
        pv.setStatus(status);
        pv.setVerifiedAt(OffsetDateTime.now());
        pv.setNotes(notes);
        PaymentVerification saved = verificationRepository.save(pv);
        // Publish event only when verification succeeded
        if (status == PaymentVerificationStatus.VERIFIED) {
            webhookPublisherService.publish(EventType.PAYMENT_VERIFIED, saved.getPayment().getId().toString());
        }
        return toDto(saved);
    }

    public List<PaymentVerificationResponse> queue() {
        return verificationRepository.findByStatusOrderByCreatedAtAsc(PaymentVerificationStatus.PENDING_QUEUE)
                .stream().map(this::toDto).toList();
    }

    public List<PaymentVerificationResponse> forVerifier(UUID verifierId) {
        return verificationRepository.findByVerifierUserId(verifierId).stream().map(this::toDto).toList();
    }

    private PaymentVerificationResponse toDto(PaymentVerification pv) {
        return PaymentVerificationResponse.builder()
                .id(pv.getId())
                .paymentId(pv.getPayment().getId())
                .verifierUserId(pv.getVerifierUserId())
                .status(pv.getStatus().name())
                .notes(pv.getNotes())
                .disputed(pv.isDisputed())
                .verifiedAt(pv.getVerifiedAt())
            .createdAt(pv.getCreatedAt() != null ? pv.getCreatedAt().atOffset(ZoneOffset.UTC) : null)
            .updatedAt(pv.getUpdatedAt() != null ? pv.getUpdatedAt().atOffset(ZoneOffset.UTC) : null)
                .build();
    }
}
