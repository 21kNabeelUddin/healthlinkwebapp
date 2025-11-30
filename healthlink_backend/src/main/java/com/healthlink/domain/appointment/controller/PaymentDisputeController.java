package com.healthlink.domain.appointment.controller;

import com.healthlink.domain.appointment.dto.CreatePaymentDisputeRequest;
import com.healthlink.domain.appointment.dto.PaymentDisputeHistoryEntry;
import com.healthlink.domain.appointment.dto.PaymentDisputeResponse;
import com.healthlink.domain.appointment.entity.PaymentDisputeResolution;
import com.healthlink.domain.appointment.service.PaymentDisputeService;
import com.healthlink.security.annotation.PhiAccess;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/disputes")
@RequiredArgsConstructor
public class PaymentDisputeController {

    private final PaymentDisputeService disputeService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @PhiAccess(reason = "raise_payment_dispute", entityType = PaymentDisputeResponse.class, idParam = "verificationId")
    public ResponseEntity<PaymentDisputeResponse> raise(@Valid @RequestBody CreatePaymentDisputeRequest request,
                                                        @RequestHeader("X-User-Id") UUID userId) {
        PaymentDisputeResponse resp = disputeService.raise(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping("/{id}/advance")
    @PreAuthorize("hasAnyRole('STAFF','DOCTOR','ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<PaymentDisputeResponse> advance(@PathVariable UUID id) {
        return ResponseEntity.ok(disputeService.advance(id));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZATION_OWNER','PLATFORM_OWNER')")
    public ResponseEntity<PaymentDisputeResponse> resolve(@PathVariable UUID id,
                                                          @RequestParam PaymentDisputeResolution resolution) {
        return ResponseEntity.ok(disputeService.resolve(id, resolution));
    }

    @GetMapping("/stage/{stage}")
    @PreAuthorize("hasAnyRole('STAFF','DOCTOR','ADMIN','ORGANIZATION_OWNER','PLATFORM_OWNER')")
    public ResponseEntity<List<PaymentDisputeResponse>> byStage(@PathVariable String stage) {
        return ResponseEntity.ok(disputeService.byStageEnum(stage));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF','DOCTOR','ADMIN','ORGANIZATION_OWNER','PLATFORM_OWNER','PATIENT')")
    public ResponseEntity<PaymentDisputeResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(disputeService.get(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('STAFF','DOCTOR','ADMIN','ORGANIZATION_OWNER','PLATFORM_OWNER')")
    public ResponseEntity<List<PaymentDisputeHistoryEntry>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(disputeService.history(id));
    }
}
