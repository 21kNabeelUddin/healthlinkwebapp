package com.healthlink.domain.appointment.controller;

import com.healthlink.domain.appointment.dto.*;
import com.healthlink.domain.appointment.entity.PaymentDisputeResolution;
import com.healthlink.domain.appointment.entity.PaymentDisputeStage;
import com.healthlink.domain.appointment.entity.PaymentVerificationStatus;
import com.healthlink.domain.appointment.service.PaymentDisputeService;
import com.healthlink.domain.appointment.service.PaymentVerificationService;
import com.healthlink.security.annotation.PhiAccess;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.healthlink.security.model.CustomUserDetails;

@RestController
@RequestMapping("/api/v1/payments/workflow")
@RequiredArgsConstructor
public class PaymentWorkflowController {

    private final PaymentVerificationService verificationService;
    private final PaymentDisputeService disputeService;

    @PostMapping("/verification/enqueue/{paymentId}")
    @PreAuthorize("hasAnyRole('STAFF','DOCTOR','ORGANIZATION')")
    public PaymentVerificationResponse enqueue(@PathVariable UUID paymentId,
                                               @RequestParam(required = false) String notes,
                                               Authentication auth) {
        UUID verifierId = extractUserId(auth);
        return verificationService.enqueue(paymentId, verifierId, notes);
    }

    @PostMapping("/verification/{id}/verify")
    @PreAuthorize("hasAnyRole('STAFF','DOCTOR','ORGANIZATION','ADMIN')")
    @PhiAccess(reason = "payment_verify", entityType = PaymentVerificationResponse.class, idParam = "id")
    public PaymentVerificationResponse verify(@PathVariable UUID id,
                                              @RequestParam PaymentVerificationStatus status,
                                              @RequestParam(required = false) String notes) {
        return verificationService.verify(id, status, notes);
    }

    @GetMapping("/verification/queue")
    @PreAuthorize("hasAnyRole('STAFF','DOCTOR','ORGANIZATION','ADMIN')")
    public List<PaymentVerificationResponse> queue() {
        return verificationService.queue();
    }

    @GetMapping("/verification/mine")
    @PreAuthorize("hasAnyRole('STAFF','DOCTOR','ORGANIZATION','ADMIN')")
    public List<PaymentVerificationResponse> mine(Authentication auth) {
        return verificationService.forVerifier(extractUserId(auth));
    }

    @PostMapping("/disputes")
    @PreAuthorize("hasAnyRole('PATIENT','STAFF','DOCTOR','ORGANIZATION')")
    @PhiAccess(reason = "payment_dispute_raise", entityType = PaymentDisputeResponse.class, idParam = "id")
    public PaymentDisputeResponse raise(@Valid @RequestBody CreatePaymentDisputeRequest request,
                                        Authentication auth) {
        return disputeService.raise(request, extractUserId(auth));
    }

    @PostMapping("/disputes/{id}/advance")
    @PreAuthorize("hasAnyRole('STAFF','DOCTOR','ORGANIZATION','ADMIN')")
    public PaymentDisputeResponse advance(@PathVariable UUID id) {
        return disputeService.advance(id);
    }

    @PostMapping("/disputes/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','PLATFORM_OWNER')")
    public PaymentDisputeResponse resolve(@PathVariable UUID id,
                                          @RequestParam PaymentDisputeResolution resolution) {
        return disputeService.resolve(id, resolution);
    }

    @GetMapping("/disputes/stage/{stage}")
    @PreAuthorize("hasAnyRole('STAFF','DOCTOR','ORGANIZATION','ADMIN')")
    public List<PaymentDisputeResponse> byStage(@PathVariable PaymentDisputeStage stage) {
        return disputeService.byStage(stage);
    }

    private UUID extractUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getId();
        }
        return null;
    }
}
