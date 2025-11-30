package com.healthlink.domain.analytics;

import com.healthlink.domain.analytics.dto.DoctorAnalyticsResponse;
import com.healthlink.domain.analytics.service.DoctorAnalyticsService;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import com.healthlink.domain.appointment.entity.PaymentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.appointment.repository.PaymentRepository;
import com.healthlink.domain.review.entity.Review;
import com.healthlink.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DoctorAnalyticsService
 */
@ExtendWith(MockitoExtension.class)
class DoctorAnalyticsServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private DoctorAnalyticsService service;

    private UUID doctorId;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
    }

    @Test
    void getDoctorAnalytics_shouldReturnCompleteAnalytics() {
        // Arrange
        when(appointmentRepository.countByDoctorId(doctorId)).thenReturn(50);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.COMPLETED)).thenReturn(40);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.CONFIRMED)).thenReturn(10);
        when(paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.VERIFIED))
                .thenReturn(new BigDecimal("50000.00"));
        when(appointmentRepository.countDistinctPatientsByDoctorId(doctorId)).thenReturn(35);

        // Act
        DoctorAnalyticsResponse response = service.getDoctorAnalytics(doctorId);

        // Assert
        assertNotNull(response);
        assertEquals(50, response.getTotalAppointments());
        assertEquals(40, response.getCompletedAppointments());
        assertEquals(10, response.getPendingAppointments());
        assertEquals(new BigDecimal("50000.00"), response.getTotalRevenue());
        assertEquals(35, response.getTotalPatients());

        verify(appointmentRepository).countByDoctorId(doctorId);
        verify(appointmentRepository).countByDoctorIdAndStatus(doctorId, AppointmentStatus.COMPLETED);
        verify(appointmentRepository).countByDoctorIdAndStatus(doctorId, AppointmentStatus.CONFIRMED);
        verify(paymentRepository).sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.VERIFIED);
        verify(appointmentRepository).countDistinctPatientsByDoctorId(doctorId);
    }

    @Test
    void getDoctorAnalytics_withNoRevenue_shouldReturnZero() {
        // Arrange
        when(appointmentRepository.countByDoctorId(doctorId)).thenReturn(5);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.COMPLETED)).thenReturn(0);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.CONFIRMED)).thenReturn(5);
        when(paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.VERIFIED)).thenReturn(null);
        when(appointmentRepository.countDistinctPatientsByDoctorId(doctorId)).thenReturn(5);
        when(reviewRepository.findByDoctorId(doctorId)).thenReturn(new ArrayList<>());

        // Act
        DoctorAnalyticsResponse response = service.getDoctorAnalytics(doctorId);

        // Assert
        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getTotalRevenue());
    }

    @Test
    void getDoctorAnalytics_withReviews_shouldCalculateAverageRating() {
        // Arrange
        List<Review> reviews = new ArrayList<>();
        reviews.add(createReview(doctorId, 5));
        reviews.add(createReview(doctorId, 4));
        reviews.add(createReview(doctorId, 5));
        reviews.add(createReview(doctorId, 3));

        when(appointmentRepository.countByDoctorId(doctorId)).thenReturn(10);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.COMPLETED)).thenReturn(8);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.CONFIRMED)).thenReturn(2);
        when(paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.VERIFIED))
                .thenReturn(new BigDecimal("10000.00"));
        when(appointmentRepository.countDistinctPatientsByDoctorId(doctorId)).thenReturn(8);
        when(reviewRepository.findByDoctorId(doctorId)).thenReturn(reviews);

        // Act
        DoctorAnalyticsResponse response = service.getDoctorAnalytics(doctorId);

        // Assert
        assertNotNull(response);
        assertEquals(4, response.getReviewCount());
        assertEquals(4.25, response.getAverageRating()); // (5+4+5+3)/4 = 4.25
        verify(reviewRepository).findByDoctorId(doctorId);
    }

    @Test
    void getDoctorAnalytics_withNoReviews_shouldReturnNullRatingAndZeroCount() {
        // Arrange
        when(appointmentRepository.countByDoctorId(doctorId)).thenReturn(5);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.COMPLETED)).thenReturn(5);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.CONFIRMED)).thenReturn(0);
        when(paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.VERIFIED))
                .thenReturn(new BigDecimal("5000.00"));
        when(appointmentRepository.countDistinctPatientsByDoctorId(doctorId)).thenReturn(5);
        when(reviewRepository.findByDoctorId(doctorId)).thenReturn(new ArrayList<>());

        // Act
        DoctorAnalyticsResponse response = service.getDoctorAnalytics(doctorId);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getReviewCount());
        assertNull(response.getAverageRating());
        verify(reviewRepository).findByDoctorId(doctorId);
    }

    @Test
    void getDoctorAnalytics_withSingleReview_shouldReturnExactRating() {
        // Arrange
        List<Review> reviews = new ArrayList<>();
        reviews.add(createReview(doctorId, 5));

        when(appointmentRepository.countByDoctorId(doctorId)).thenReturn(1);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.COMPLETED)).thenReturn(1);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.CONFIRMED)).thenReturn(0);
        when(paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.VERIFIED))
                .thenReturn(new BigDecimal("1000.00"));
        when(appointmentRepository.countDistinctPatientsByDoctorId(doctorId)).thenReturn(1);
        when(reviewRepository.findByDoctorId(doctorId)).thenReturn(reviews);

        // Act
        DoctorAnalyticsResponse response = service.getDoctorAnalytics(doctorId);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getReviewCount());
        assertEquals(5.0, response.getAverageRating());
    }

    @Test
    void getDoctorAnalytics_withMixedRatings_shouldCalculateCorrectAverage() {
        // Arrange
        List<Review> reviews = new ArrayList<>();
        reviews.add(createReview(doctorId, 5));
        reviews.add(createReview(doctorId, 1));
        reviews.add(createReview(doctorId, 3));

        when(appointmentRepository.countByDoctorId(doctorId)).thenReturn(3);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.COMPLETED)).thenReturn(3);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.CONFIRMED)).thenReturn(0);
        when(paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.VERIFIED))
                .thenReturn(new BigDecimal("3000.00"));
        when(appointmentRepository.countDistinctPatientsByDoctorId(doctorId)).thenReturn(3);
        when(reviewRepository.findByDoctorId(doctorId)).thenReturn(reviews);

        // Act
        DoctorAnalyticsResponse response = service.getDoctorAnalytics(doctorId);

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getReviewCount());
        assertEquals(3.0, response.getAverageRating()); // (5+1+3)/3 = 3.0
    }

    @Test
    void getDoctorAnalytics_withAllPerfectRatings_shouldReturnFive() {
        // Arrange
        List<Review> reviews = new ArrayList<>();
        reviews.add(createReview(doctorId, 5));
        reviews.add(createReview(doctorId, 5));
        reviews.add(createReview(doctorId, 5));
        reviews.add(createReview(doctorId, 5));
        reviews.add(createReview(doctorId, 5));

        when(appointmentRepository.countByDoctorId(doctorId)).thenReturn(5);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.COMPLETED)).thenReturn(5);
        when(appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.CONFIRMED)).thenReturn(0);
        when(paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.VERIFIED))
                .thenReturn(new BigDecimal("5000.00"));
        when(appointmentRepository.countDistinctPatientsByDoctorId(doctorId)).thenReturn(5);
        when(reviewRepository.findByDoctorId(doctorId)).thenReturn(reviews);

        // Act
        DoctorAnalyticsResponse response = service.getDoctorAnalytics(doctorId);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getReviewCount());
        assertEquals(5.0, response.getAverageRating());
    }

    // Helper method to create review
    private Review createReview(UUID doctorId, int rating) {
        Review review = new Review();
        review.setId(UUID.randomUUID());
        review.setDoctorId(doctorId);
        review.setPatientId(UUID.randomUUID());
        review.setRating(rating);
        return review;
    }
}
