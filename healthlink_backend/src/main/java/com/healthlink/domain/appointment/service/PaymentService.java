package com.healthlink.domain.appointment.service;

import com.healthlink.domain.appointment.dto.InitiatePaymentRequest;
import com.healthlink.domain.appointment.dto.PaymentResponse;
import com.healthlink.domain.appointment.dto.VerifyPaymentRequest;
import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import com.healthlink.domain.appointment.entity.Payment;
import com.healthlink.domain.appointment.entity.PaymentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.appointment.repository.PaymentRepository;
import com.healthlink.domain.notification.NotificationType;
import com.healthlink.domain.notification.service.NotificationSchedulerService;
import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.service.payment.PaymentAccountResolver;
import com.healthlink.service.webhook.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final NotificationSchedulerService notificationSchedulerService;
    private final PaymentAccountResolver paymentAccountResolver;
    private final WebhookService webhookService;

    public PaymentResponse initiatePayment(InitiatePaymentRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // Resolve payment account based on organization's payment mode
        UUID doctorId = appointment.getDoctor().getId();
        UUID organizationId = null; // Get from appointment or doctor if available

        PaymentAccountResolver.PaymentAccountDetails accountDetails = paymentAccountResolver
                .resolvePaymentAccount(doctorId, organizationId);

        log.info("Payment routing resolved: {} for doctor: {}",
                accountDetails.getAccountHolderType(), doctorId);

        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setAmount(request.getAmount());
        payment.setMethod(request.getMethod());
        payment.setStatus(PaymentStatus.PENDING_VERIFICATION);
        payment.setCurrency("PKR");

        Payment saved = paymentRepository.save(payment);

        // Emit webhook for payment initiated
        webhookService.emitEvent(WebhookService.WebhookEvent.builder()
                .eventType(WebhookService.EventTypes.PAYMENT_INITIATED)
                .entityId(saved.getId().toString())
                .entityType("Payment")
                .data(Map.of(
                        "amount", saved.getAmount().toString(),
                        "accountHolderType", accountDetails.getAccountHolderType(),
                        "appointmentId", appointment.getId().toString()))
                .build());

        return mapToResponse(saved);
    }

    public PaymentResponse uploadReceipt(UUID paymentId, String receiptUrl) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setReceiptUrl(receiptUrl);
        Payment savedPayment = paymentRepository.save(payment);

        // Notify Staff
        UUID doctorId = payment.getAppointment().getDoctor().getId();
        java.util.List<User> staffList = userRepository.findStaffByDoctorId(doctorId);

        for (User staff : staffList) {
            notificationSchedulerService.scheduleNotification(
                    staff.getId(),
                    NotificationType.PAYMENT_VERIFICATION,
                    "Payment Receipt Uploaded",
                    "A patient has uploaded a payment receipt for verification.",
                    java.util.Map.of("paymentId", paymentId.toString()));
        }

        return mapToResponse(savedPayment);
    }

    public PaymentResponse verifyPayment(VerifyPaymentRequest request, String verifierEmail) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        var verifier = userRepository.findByEmail(verifierEmail)
                .orElseThrow(() -> new RuntimeException("Verifier not found"));

        // Transition logic
        PaymentStatus target = request.getStatus();
        if (target == PaymentStatus.VERIFIED && payment.getStatus() == PaymentStatus.PENDING_VERIFICATION) {
            payment.setStatus(PaymentStatus.VERIFIED);
        } else if (target == PaymentStatus.REJECTED && payment.getStatus() == PaymentStatus.PENDING_VERIFICATION) {
            payment.setStatus(PaymentStatus.REJECTED);
        } else if (target == PaymentStatus.AUTHORIZED && payment.getStatus() == PaymentStatus.PENDING_VERIFICATION) {
            payment.setStatus(PaymentStatus.AUTHORIZED);
        } else if (target == PaymentStatus.CAPTURED
                && (payment.getStatus() == PaymentStatus.AUTHORIZED || payment.getStatus() == PaymentStatus.VERIFIED)) {
            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setCapturedAt(LocalDateTime.now());
        } else if (target == PaymentStatus.FAILED) {
            payment.setStatus(PaymentStatus.FAILED);
        } else {
            throw new RuntimeException("Invalid payment status transition");
        }
        payment.setAttemptCount(payment.getAttemptCount() + 1);
        payment.setLastAttemptAt(LocalDateTime.now());
        payment.setVerifiedByUserId(verifier.getId());
        payment.setVerificationNotes(request.getVerificationNotes());
        payment.setVerifiedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        // Update appointment status based on payment verification
        if (payment.getStatus() == PaymentStatus.VERIFIED || payment.getStatus() == PaymentStatus.CAPTURED) {
            Appointment appointment = payment.getAppointment();
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointmentRepository.save(appointment);

            // Emit webhook for successful verification
            webhookService.emitEvent(WebhookService.WebhookEvent.builder()
                    .eventType(WebhookService.EventTypes.PAYMENT_VERIFIED)
                    .entityId(savedPayment.getId().toString())
                    .entityType("Payment")
                    .data(Map.of(
                            "appointmentId", appointment.getId().toString(),
                            "amount", savedPayment.getAmount().toString(),
                            "status", savedPayment.getStatus().name()))
                    .build());
        } else if (payment.getStatus() == PaymentStatus.FAILED) {
            // Emit webhook for failed payment
            webhookService.emitEvent(WebhookService.WebhookEvent.builder()
                    .eventType(WebhookService.EventTypes.PAYMENT_FAILED)
                    .entityId(savedPayment.getId().toString())
                    .entityType("Payment")
                    .data(Map.of(
                            "reason",
                            request.getVerificationNotes() != null ? request.getVerificationNotes() : "Unknown"))
                    .build());
        }

        return mapToResponse(savedPayment);
    }

    public java.util.List<PaymentResponse> getPayments(String actorId, boolean isDoctorView, String username) {
        var user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        java.util.List<Payment> payments;
        if (isDoctorView) {
            // If doctor view, ensure the user is the doctor or admin
            // For simplicity, assuming actorId is the doctor's ID if provided, else use
            // logged in user's ID if they are a doctor
            UUID doctorId = actorId != null ? UUID.fromString(actorId) : user.getId();
            payments = paymentRepository.findByAppointmentDoctorId(doctorId);
        } else {
            // Patient view
            UUID patientId = actorId != null ? UUID.fromString(actorId) : user.getId();
            payments = paymentRepository.findByAppointmentPatientId(patientId);
        }

        return payments.stream().map(this::mapToResponse).toList();
    }

    public void requestRefund(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.CAPTURED || payment.getStatus() == PaymentStatus.VERIFIED) {
            payment.setStatus(PaymentStatus.REFUND_REQUESTED);
            paymentRepository.save(payment);
        } else {
            throw new RuntimeException("Cannot request refund for unverified payment");
        }
    }

    public PaymentResponse completeRefund(UUID paymentId, String notes) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        if (payment.getStatus() != PaymentStatus.REFUND_REQUESTED) {
            throw new RuntimeException("Refund can only be completed from REFUND_REQUESTED state");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) {
            payment.setVerificationNotes(
                    (payment.getVerificationNotes() == null ? "" : payment.getVerificationNotes() + " | ") + notes);
        }
        paymentRepository.save(payment);
        return mapToResponse(payment);
    }

    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return mapToResponse(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .appointmentId(payment.getAppointment().getId())
                .patientId(payment.getAppointment().getPatient().getId().toString())
                .doctorId(payment.getAppointment().getDoctor().getId().toString())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .method(payment.getMethod().name())
                .externalProvider(payment.getExternalProvider())
                .externalStatus(payment.getExternalStatus())
                .attemptCount(payment.getAttemptCount())
                .capturedAt(payment.getCapturedAt())
                .refundedAt(payment.getRefundedAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
