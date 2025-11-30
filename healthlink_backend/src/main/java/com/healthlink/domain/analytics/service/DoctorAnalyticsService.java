package com.healthlink.domain.analytics.service;

import com.healthlink.domain.analytics.dto.DoctorAnalyticsResponse;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.appointment.repository.PaymentRepository;
import com.healthlink.domain.review.repository.ReviewRepository;
import com.healthlink.domain.review.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorAnalyticsService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;

    public DoctorAnalyticsResponse getDoctorAnalytics(UUID doctorId) {
        Integer totalAppointments = appointmentRepository.countByDoctorId(doctorId);
        Integer completedAppointments = appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.COMPLETED);
        Integer pendingAppointments = appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.CONFIRMED);
        
        BigDecimal totalRevenue = paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, com.healthlink.domain.appointment.entity.PaymentStatus.VERIFIED);
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }
        
        Integer totalPatients = appointmentRepository.countDistinctPatientsByDoctorId(doctorId);
        
        // Calculate average rating and review count
        List<Review> reviews = reviewRepository.findByDoctorId(doctorId);
        Integer reviewCount = reviews.size();
        Double averageRating = reviews.isEmpty() ? null : 
            reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .getAsDouble();

        return DoctorAnalyticsResponse.builder()
                .totalAppointments(totalAppointments)
                .completedAppointments(completedAppointments)
                .pendingAppointments(pendingAppointments)
                .totalRevenue(totalRevenue)
                .totalPatients(totalPatients)
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .build();
    }
}
