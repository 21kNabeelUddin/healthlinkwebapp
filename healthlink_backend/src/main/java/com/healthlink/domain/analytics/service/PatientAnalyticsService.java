package com.healthlink.domain.analytics.service;

import com.healthlink.domain.analytics.dto.PatientAnalyticsResponse;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.appointment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientAnalyticsService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;

    public PatientAnalyticsResponse getPatientAnalytics(UUID patientId) {
        Integer totalAppointments = appointmentRepository.countByPatientId(patientId);
        Integer completedAppointments = appointmentRepository.countByPatientIdAndStatus(patientId, AppointmentStatus.COMPLETED);
        Integer cancelledAppointments = appointmentRepository.countByPatientIdAndStatus(patientId, AppointmentStatus.CANCELLED);
        
        BigDecimal totalPayments = paymentRepository.sumAmountByPatientId(patientId);
        if (totalPayments == null) {
            totalPayments = BigDecimal.ZERO;
        }
        
        Integer uniqueDoctors = appointmentRepository.countDistinctDoctorsByPatientId(patientId);

        return PatientAnalyticsResponse.builder()
                .totalAppointments(totalAppointments)
                .completedAppointments(completedAppointments)
                .cancelledAppointments(cancelledAppointments)
                .totalPayments(totalPayments)
                .uniqueDoctorsVisited(uniqueDoctors)
                .build();
    }
}
