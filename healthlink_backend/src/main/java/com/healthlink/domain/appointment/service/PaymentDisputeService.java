package com.healthlink.domain.appointment.service;

import com.healthlink.domain.appointment.dto.CreatePaymentDisputeRequest;
import com.healthlink.domain.appointment.dto.PaymentDisputeResponse;
import com.healthlink.domain.appointment.dto.PaymentDisputeHistoryEntry;
import com.healthlink.domain.appointment.entity.*;
import com.healthlink.domain.appointment.repository.PaymentDisputeHistoryRepository;
import com.healthlink.domain.appointment.repository.PaymentDisputeRepository;
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
public class PaymentDisputeService {

    private final PaymentDisputeRepository disputeRepository;
    private final PaymentDisputeHistoryRepository historyRepository;
    private final PaymentVerificationRepository verificationRepository;
    private final WebhookPublisherService webhookPublisherService;

    public PaymentDisputeResponse raise(CreatePaymentDisputeRequest request, UUID raisedByUserId) {
        PaymentVerification verification = verificationRepository.findById(request.getVerificationId())
                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));
        verification.setDisputed(true);
        PaymentDispute dispute = new PaymentDispute();
        dispute.setId(UUID.randomUUID()); // ensure ID pre-save for downstream publishing and DTO
        dispute.setVerification(verification);
        dispute.setStage(PaymentDisputeStage.STAFF_REVIEW); // initial stage
        dispute.setRaisedByUserId(raisedByUserId);
        dispute.setNotes(request.getNotes());
        PaymentDispute saved = disputeRepository.save(dispute);
        // history entry for initial creation
        PaymentDisputeHistory h = new PaymentDisputeHistory();
        h.setDispute(saved);
        h.setFromStage(null);
        h.setToStage(PaymentDisputeStage.STAFF_REVIEW);
        h.setChangedByUserId(raisedByUserId);
        h.setNote(request.getNotes());
        historyRepository.save(h);
        webhookPublisherService.publish(EventType.PAYMENT_DISPUTED, saved.getId().toString());
        return toDto(saved);
    }

    public PaymentDisputeResponse advance(UUID disputeId) {
        PaymentDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
        if (dispute.getResolvedAt() != null) {
            throw new IllegalStateException("Cannot advance a resolved dispute");
        }
        PaymentDisputeStage current = dispute.getStage();
        PaymentDisputeStage next = nextStage(current);
        if (next == current) {
            throw new IllegalStateException("Dispute already at final stage");
        }
        dispute.setStage(next);
        PaymentDispute saved = disputeRepository.save(dispute);
        PaymentDisputeHistory h = new PaymentDisputeHistory();
        h.setDispute(saved);
        h.setFromStage(current);
        h.setToStage(next);
        h.setChangedByUserId(dispute.getRaisedByUserId()); // fallback; real impl would use auth principal
        h.setNote("advanced");
        historyRepository.save(h);
        return toDto(saved);
    }

    public PaymentDisputeResponse resolve(UUID disputeId, PaymentDisputeResolution resolution) {
        PaymentDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
        if (dispute.getResolvedAt() != null) {
            throw new IllegalStateException("Dispute already resolved");
        }
        dispute.setResolutionStatus(resolution);
        dispute.setResolvedAt(OffsetDateTime.now());
        PaymentDispute saved = disputeRepository.save(dispute);
        PaymentDisputeHistory h = new PaymentDisputeHistory();
        h.setDispute(saved);
        h.setFromStage(dispute.getStage());
        h.setToStage(dispute.getStage());
        h.setChangedByUserId(dispute.getRaisedByUserId());
        h.setResolutionStatus(resolution);
        h.setNote("resolved:" + resolution.name());
        historyRepository.save(h);
        return toDto(saved);
    }

    public List<PaymentDisputeResponse> byStage(PaymentDisputeStage stage) {
        return disputeRepository.findByStageOrderByCreatedAtAsc(stage).stream().map(this::toDto).toList();
    }

    public List<PaymentDisputeResponse> byStageEnum(String stage) {
        PaymentDisputeStage st = PaymentDisputeStage.valueOf(stage.toUpperCase());
        return byStage(st);
    }

    private PaymentDisputeStage nextStage(PaymentDisputeStage current) {
        return switch (current) {
            case STAFF_REVIEW -> PaymentDisputeStage.PRACTICE_OWNER_REVIEW;
            case PRACTICE_OWNER_REVIEW -> PaymentDisputeStage.ADMIN_REVIEW;
            case ADMIN_REVIEW -> PaymentDisputeStage.ADMIN_REVIEW; // terminal stage
        };
    }

    public List<PaymentDisputeHistoryEntry> history(UUID disputeId) {
        PaymentDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
        return historyRepository.findByDispute_IdOrderByCreatedAtAsc(dispute.getId())
                .stream().map(this::toHistoryDto).toList();
    }

    private PaymentDisputeHistoryEntry toHistoryDto(PaymentDisputeHistory h) {
        return PaymentDisputeHistoryEntry.builder()
                .id(h.getId())
                .disputeId(h.getDispute().getId())
                .fromStage(h.getFromStage() != null ? h.getFromStage().name() : null)
                .toStage(h.getToStage() != null ? h.getToStage().name() : null)
                .changedByUserId(h.getChangedByUserId())
                .note(h.getNote())
                .resolutionStatus(h.getResolutionStatus() != null ? h.getResolutionStatus().name() : null)
                .createdAt(h.getCreatedAt() != null ? h.getCreatedAt().atOffset(ZoneOffset.UTC) : null)
                .build();
    }

    public PaymentDisputeResponse get(UUID id) {
        PaymentDispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
        return toDto(dispute);
    }

    private PaymentDisputeResponse toDto(PaymentDispute d) {
        return PaymentDisputeResponse.builder()
                .id(d.getId())
                .verificationId(d.getVerification().getId())
                .stage(d.getStage().name())
                .resolutionStatus(d.getResolutionStatus().name())
                .raisedByUserId(d.getRaisedByUserId())
                .notes(d.getNotes())
                .resolvedAt(d.getResolvedAt())
            .createdAt(d.getCreatedAt() != null ? d.getCreatedAt().atOffset(ZoneOffset.UTC) : null)
            .updatedAt(d.getUpdatedAt() != null ? d.getUpdatedAt().atOffset(ZoneOffset.UTC) : null)
                .build();
    }
}
