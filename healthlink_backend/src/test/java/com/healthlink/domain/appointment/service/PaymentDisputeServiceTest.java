package com.healthlink.domain.appointment.service;

import com.healthlink.domain.appointment.dto.CreatePaymentDisputeRequest;
import com.healthlink.domain.appointment.entity.PaymentDisputeStage;
import com.healthlink.domain.appointment.entity.PaymentDisputeResolution;
import com.healthlink.domain.appointment.entity.PaymentVerification;
import com.healthlink.domain.appointment.repository.PaymentDisputeHistoryRepository;
import com.healthlink.domain.appointment.repository.PaymentDisputeRepository;
import com.healthlink.domain.appointment.repository.PaymentVerificationRepository;
import com.healthlink.domain.webhook.WebhookPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class PaymentDisputeServiceTest {

    private PaymentDisputeRepository disputeRepository;
    private PaymentDisputeHistoryRepository historyRepository;
    private PaymentVerificationRepository verificationRepository;
    private WebhookPublisherService webhookPublisherService;
    private PaymentDisputeService service;

    @BeforeEach
    void setup() {
        disputeRepository = mock(PaymentDisputeRepository.class);
        historyRepository = mock(PaymentDisputeHistoryRepository.class);
        verificationRepository = mock(PaymentVerificationRepository.class);
        webhookPublisherService = mock(WebhookPublisherService.class);
        service = new PaymentDisputeService(disputeRepository, historyRepository, verificationRepository, webhookPublisherService);
    }

    @Test
    void raise_setsInitialStageStaff() {
        UUID verificationId = UUID.randomUUID();
        PaymentVerification pv = new PaymentVerification();
        when(verificationRepository.findById(verificationId)).thenReturn(Optional.of(pv));
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreatePaymentDisputeRequest req = new CreatePaymentDisputeRequest();
        req.setVerificationId(verificationId);
        req.setNotes("Issue with payment amount");
        var dto = service.raise(req, UUID.randomUUID());
        assertThat(dto.getStage()).isEqualTo(PaymentDisputeStage.STAFF_REVIEW.name());
    }

    @Test
    void advance_goesToPracticeOwner_then_Admin_then_stops() {
        // Setup a dispute at STAFF_REVIEW
        var dispute = TestFixtures.newDispute(PaymentDisputeStage.STAFF_REVIEW);
        when(disputeRepository.findById(dispute.getId())).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // First advance
        var first = service.advance(dispute.getId());
        assertThat(first.getStage()).isEqualTo(PaymentDisputeStage.PRACTICE_OWNER_REVIEW.name());
        dispute.setStage(PaymentDisputeStage.PRACTICE_OWNER_REVIEW);
        // Second advance
        var second = service.advance(dispute.getId());
        assertThat(second.getStage()).isEqualTo(PaymentDisputeStage.ADMIN_REVIEW.name());
        dispute.setStage(PaymentDisputeStage.ADMIN_REVIEW);
        // Third advance should fail (terminal)
        assertThrows(IllegalStateException.class, () -> service.advance(dispute.getId()));
    }

    @Test
    void resolve_setsResolution_andTimestamp() {
        var dispute = TestFixtures.newDispute(PaymentDisputeStage.ADMIN_REVIEW);
        when(disputeRepository.findById(dispute.getId())).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var dto = service.resolve(dispute.getId(), PaymentDisputeResolution.UPHELD);
        assertThat(dto.getResolutionStatus()).isEqualTo(PaymentDisputeResolution.UPHELD.name());
        assertThat(dto.getResolvedAt()).isNotNull();
    }

    static class TestFixtures {
        static com.healthlink.domain.appointment.entity.PaymentDispute newDispute(PaymentDisputeStage stage) {
            var d = new com.healthlink.domain.appointment.entity.PaymentDispute();
            d.setId(UUID.randomUUID());
            d.setStage(stage);
            var v = new PaymentVerification();
            v.setId(UUID.randomUUID());
            d.setVerification(v);
            d.setRaisedByUserId(UUID.randomUUID());
            return d;
        }
    }
}
