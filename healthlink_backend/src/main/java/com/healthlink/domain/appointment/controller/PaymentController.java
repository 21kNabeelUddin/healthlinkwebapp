package com.healthlink.domain.appointment.controller;

import com.healthlink.domain.appointment.dto.InitiatePaymentRequest;
import com.healthlink.domain.appointment.dto.PaymentResponse;
import com.healthlink.domain.appointment.dto.VerifyPaymentRequest;
import com.healthlink.domain.appointment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Initiate a payment")
    @ApiResponse(responseCode = "200", description = "Payment initiated")
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }

    @PostMapping("/{paymentId}/receipt")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Upload payment receipt URL")
    @ApiResponse(responseCode = "200", description = "Receipt uploaded")
    public ResponseEntity<PaymentResponse> uploadReceipt(
            @PathVariable UUID paymentId,
            @RequestParam String receiptUrl) {
        return ResponseEntity.ok(paymentService.uploadReceipt(paymentId, receiptUrl));
    }

    @GetMapping("/{paymentId}/receipt")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    @Operation(summary = "Get payment receipt")
    @ApiResponse(responseCode = "200", description = "Receipt found")
    public ResponseEntity<PaymentResponse> getReceipt(@PathVariable UUID paymentId) {
        // For now, returning the payment details which includes receipt URL
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('DOCTOR', 'STAFF')")
    @Operation(summary = "Verify a payment")
    @ApiResponse(responseCode = "200", description = "Payment verified")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(paymentService.verifyPayment(request, authentication.getName()));
    }

    @PostMapping("/{paymentId}/authorize")
    @PreAuthorize("hasAnyRole('DOCTOR','STAFF')")
    @Operation(summary = "Authorize a payment prior to capture")
    @ApiResponse(responseCode = "200", description = "Payment authorized")
    public ResponseEntity<PaymentResponse> authorizePayment(@PathVariable UUID paymentId,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setPaymentId(paymentId);
        req.setStatus(com.healthlink.domain.appointment.entity.PaymentStatus.AUTHORIZED);
        req.setVerificationNotes(notes);
        return ResponseEntity.ok(paymentService.verifyPayment(req, authentication.getName()));
    }

    @PostMapping("/{paymentId}/capture")
    @PreAuthorize("hasAnyRole('DOCTOR','STAFF')")
    @Operation(summary = "Capture an authorized/verified payment")
    @ApiResponse(responseCode = "200", description = "Payment captured")
    public ResponseEntity<PaymentResponse> capturePayment(@PathVariable UUID paymentId,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setPaymentId(paymentId);
        req.setStatus(com.healthlink.domain.appointment.entity.PaymentStatus.CAPTURED);
        req.setVerificationNotes(notes);
        return ResponseEntity.ok(paymentService.verifyPayment(req, authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    @Operation(summary = "Get payment history")
    @ApiResponse(responseCode = "200", description = "Payment history retrieved")
    public ResponseEntity<java.util.List<PaymentResponse>> getPayments(
            @RequestParam(required = false) String actorId,
            @RequestParam(defaultValue = "false") boolean isDoctorView,
            Authentication authentication) {
        return ResponseEntity.ok(paymentService.getPayments(actorId, isDoctorView, authentication.getName()));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Request a refund")
    @ApiResponse(responseCode = "200", description = "Refund requested")
    public ResponseEntity<Void> requestRefund(@PathVariable UUID id) {
        paymentService.requestRefund(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/refund/complete")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Complete a requested refund")
    @ApiResponse(responseCode = "200", description = "Refund completed")
    public ResponseEntity<PaymentResponse> completeRefund(@PathVariable UUID id,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(paymentService.completeRefund(id, notes));
    }
}
