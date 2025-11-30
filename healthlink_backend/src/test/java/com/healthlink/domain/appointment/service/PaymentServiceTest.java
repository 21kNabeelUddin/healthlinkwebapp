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
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.service.payment.PaymentAccountResolver;
import com.healthlink.service.webhook.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for PaymentService
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

        @Mock
        private PaymentRepository paymentRepository;

        @Mock
        private AppointmentRepository appointmentRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private PaymentAccountResolver paymentAccountResolver;

        @Mock
        private WebhookService webhookService;

        @Mock
        private com.healthlink.domain.notification.service.NotificationSchedulerService notificationSchedulerService;

        @InjectMocks
        private PaymentService paymentService;

        private Appointment testAppointment;
        private Payment testPayment;
        private Patient testPatient;
        private Doctor testDoctor;

        @BeforeEach
        void setUp() {
                testDoctor = new Doctor();
                testDoctor.setId(UUID.randomUUID());

                testPatient = new Patient();
                testPatient.setId(UUID.randomUUID());
                testPatient.setEmail("patient@test.com");

                testAppointment = new Appointment();
                testAppointment.setId(UUID.randomUUID());
                testAppointment.setDoctor(testDoctor);
                testAppointment.setPatient(testPatient);

                testPayment = new Payment();
                testPayment.setId(UUID.randomUUID());
                testPayment.setAppointment(testAppointment);
                testPayment.setAmount(new BigDecimal("5000"));
                testPayment.setMethod(com.healthlink.domain.appointment.entity.PaymentMethod.BANK_TRANSFER);
                testPayment.setStatus(PaymentStatus.PENDING_VERIFICATION);
        }

        @Test
        void initiatePayment_shouldCreatePaymentSuccessfully() {
                InitiatePaymentRequest request = new InitiatePaymentRequest();
                request.setAppointmentId(testAppointment.getId());
                request.setAmount(new BigDecimal("5000"));
                request.setMethod(com.healthlink.domain.appointment.entity.PaymentMethod.BANK_TRANSFER);

                PaymentAccountResolver.PaymentAccountDetails accountDetails = new PaymentAccountResolver.PaymentAccountDetails();
                accountDetails.setAccountHolderType("DOCTOR");

                when(appointmentRepository.findById(testAppointment.getId()))
                                .thenReturn(Optional.of(testAppointment));
                when(paymentAccountResolver.resolvePaymentAccount(any(), nullable(UUID.class)))
                                .thenReturn(accountDetails);
                when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
                        Payment p = i.getArgument(0);
                        p.setId(UUID.randomUUID());
                        return p;
                });

                PaymentResponse response = paymentService.initiatePayment(request);

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING_VERIFICATION.name());
                verify(paymentRepository).save(any(Payment.class));
                verify(webhookService).emitEvent(any());
        }

        @Test
        void initiatePayment_shouldThrowWhenAppointmentNotFound() {
                InitiatePaymentRequest request = new InitiatePaymentRequest();
                request.setAppointmentId(UUID.randomUUID());

                when(appointmentRepository.findById(any())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> paymentService.initiatePayment(request))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Appointment not found");
        }

        @Test
        void uploadReceipt_shouldUpdateReceiptUrl() {
                String receiptUrl = "https://s3.amazonaws.com/receipts/123.jpg";

                when(paymentRepository.findById(testPayment.getId()))
                                .thenReturn(Optional.of(testPayment));
                when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
                when(userRepository.findStaffByDoctorId(any())).thenReturn(java.util.List.of());

                PaymentResponse response = paymentService.uploadReceipt(testPayment.getId(), receiptUrl);

                assertThat(response).isNotNull();
                assertThat(testPayment.getReceiptUrl()).isEqualTo(receiptUrl);
        }

        @Test
        void verifyPayment_shouldTransitionToVerified() {
                VerifyPaymentRequest request = new VerifyPaymentRequest();
                request.setPaymentId(testPayment.getId());
                request.setStatus(PaymentStatus.VERIFIED);

                com.healthlink.domain.user.entity.Staff verifier = new com.healthlink.domain.user.entity.Staff();
                verifier.setId(UUID.randomUUID());
                verifier.setEmail("staff@test.com");

                when(paymentRepository.findById(testPayment.getId()))
                                .thenReturn(Optional.of(testPayment));
                when(userRepository.findByEmail("staff@test.com"))
                                .thenReturn(Optional.of(verifier));
                when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
                when(appointmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                PaymentResponse response = paymentService.verifyPayment(request, "staff@test.com");

                assertThat(response.getStatus()).isEqualTo(PaymentStatus.VERIFIED.name());
                assertThat(testAppointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
                verify(webhookService).emitEvent(any());
        }

        @Test
        void verifyPayment_shouldTransitionToRejected() {
                VerifyPaymentRequest request = new VerifyPaymentRequest();
                request.setPaymentId(testPayment.getId());
                request.setStatus(PaymentStatus.REJECTED);

                com.healthlink.domain.user.entity.Staff verifier = new com.healthlink.domain.user.entity.Staff();
                verifier.setId(UUID.randomUUID());
                verifier.setEmail("staff@test.com");

                when(paymentRepository.findById(testPayment.getId()))
                                .thenReturn(Optional.of(testPayment));
                when(userRepository.findByEmail("staff@test.com"))
                                .thenReturn(Optional.of(verifier));
                when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                PaymentResponse response = paymentService.verifyPayment(request, "staff@test.com");

                assertThat(response.getStatus()).isEqualTo(PaymentStatus.REJECTED.name());
                verify(appointmentRepository, never()).save(any()); // Appointment should not be updated
        }

        @Test
        void verifyPayment_shouldThrowForInvalidTransition() {
                testPayment.setStatus(PaymentStatus.CAPTURED); // Already captured
                VerifyPaymentRequest request = new VerifyPaymentRequest();
                request.setPaymentId(testPayment.getId());
                request.setStatus(PaymentStatus.VERIFIED);

                com.healthlink.domain.user.entity.Staff verifier = new com.healthlink.domain.user.entity.Staff();
                verifier.setEmail("staff@test.com");

                when(paymentRepository.findById(testPayment.getId()))
                                .thenReturn(Optional.of(testPayment));
                when(userRepository.findByEmail("staff@test.com"))
                                .thenReturn(Optional.of(verifier));

                assertThatThrownBy(() -> paymentService.verifyPayment(request, "staff@test.com"))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Invalid payment status transition");
        }

        @Test
        void requestRefund_shouldUpdateStatusToRefundRequested() {
                testPayment.setStatus(PaymentStatus.VERIFIED);

                when(paymentRepository.findById(testPayment.getId()))
                                .thenReturn(Optional.of(testPayment));
                when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                paymentService.requestRefund(testPayment.getId());

                assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.REFUND_REQUESTED);
        }

        @Test
        void requestRefund_shouldThrowForUnverifiedPayment() {
                testPayment.setStatus(PaymentStatus.PENDING_VERIFICATION);

                when(paymentRepository.findById(testPayment.getId()))
                                .thenReturn(Optional.of(testPayment));

                assertThatThrownBy(() -> paymentService.requestRefund(testPayment.getId()))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("unverified payment");
        }

        @Test
        void completeRefund_shouldUpdateStatusToRefunded() {
                testPayment.setStatus(PaymentStatus.REFUND_REQUESTED);

                when(paymentRepository.findById(testPayment.getId()))
                                .thenReturn(Optional.of(testPayment));
                when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                PaymentResponse response = paymentService.completeRefund(testPayment.getId(), "Customer request");

                assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED.name());
                assertThat(testPayment.getRefundedAt()).isNotNull();
        }

        @Test
        void completeRefund_shouldThrowForInvalidState() {
                testPayment.setStatus(PaymentStatus.PENDING_VERIFICATION);

                when(paymentRepository.findById(testPayment.getId()))
                                .thenReturn(Optional.of(testPayment));

                assertThatThrownBy(() -> paymentService.completeRefund(testPayment.getId(), "notes"))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("REFUND_REQUESTED state");
        }

        @Test
        void getPayment_shouldReturnPaymentDetails() {
                when(paymentRepository.findById(testPayment.getId()))
                                .thenReturn(Optional.of(testPayment));

                PaymentResponse response = paymentService.getPayment(testPayment.getId());

                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(testPayment.getId());
        }
}
