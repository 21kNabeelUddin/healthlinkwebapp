package com.healthlink.domain.analytics.service;

import com.healthlink.domain.analytics.dto.DoctorAnalyticsResponse;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import com.healthlink.domain.appointment.entity.PaymentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.appointment.repository.PaymentRepository;
import com.healthlink.domain.review.entity.Review;
import com.healthlink.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorAnalyticsTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private DoctorAnalyticsService doctorAnalyticsService;

    @Test
    void shouldCalculateAnalyticsCorrectly() {
        // Given
        UUID doctorId = UUID.randomUUID();

        when(appointmentRepository.countByDoctorId(doctorId)).thenReturn(10);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.COMPLETED)).thenReturn(5);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.CONFIRMED)).thenReturn(2);
        when(paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.VERIFIED))
                .thenReturn(new BigDecimal("5000.00"));
        when(appointmentRepository.countDistinctPatientsByDoctorId(doctorId)).thenReturn(8);

        Review review1 = new Review();
        review1.setRating(5);
        Review review2 = new Review();
        review2.setRating(4);
        when(reviewRepository.findByDoctorId(doctorId)).thenReturn(List.of(review1, review2));

        // When
        DoctorAnalyticsResponse response = doctorAnalyticsService.getDoctorAnalytics(doctorId);

        // Then
        assertThat(response.getTotalAppointments()).isEqualTo(10);
        assertThat(response.getCompletedAppointments()).isEqualTo(5);
        assertThat(response.getPendingAppointments()).isEqualTo(2);
        assertThat(response.getTotalRevenue()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(response.getTotalPatients()).isEqualTo(8);
        assertThat(response.getReviewCount()).isEqualTo(2);
        assertThat(response.getAverageRating()).isEqualTo(4.5);
    }
}
