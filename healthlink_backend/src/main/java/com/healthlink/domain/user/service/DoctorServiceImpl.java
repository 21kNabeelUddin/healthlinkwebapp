package com.healthlink.domain.user.service;

import com.healthlink.domain.appointment.entity.PaymentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.appointment.repository.PaymentRepository;
import com.healthlink.domain.user.dto.DoctorDashboardDTO;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorServiceImpl implements DoctorService {

        private final DoctorRepository doctorRepository;
        private final PaymentRepository paymentRepository;
        private final AppointmentRepository appointmentRepository;

        @Override
        public DoctorDashboardDTO getDashboard(UUID doctorId) {
                Doctor doctor = doctorRepository.findById(doctorId)
                                .orElseThrow(() -> new RuntimeException("Doctor not found"));

                BigDecimal verified = paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.VERIFIED);
                BigDecimal captured = paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.CAPTURED);

                BigDecimal totalRevenue = (verified != null ? verified : BigDecimal.ZERO)
                                .add(captured != null ? captured : BigDecimal.ZERO);

                Integer totalAppointments = appointmentRepository.countByDoctorId(doctorId);

                return DoctorDashboardDTO.builder()
                                .totalRevenue(totalRevenue)
                                .totalAppointments(totalAppointments)
                                .averageRating(doctor.getAverageRating())
                                .totalReviews(doctor.getTotalReviews())
                                .build();
        }

        @Override
        public Doctor getDoctorById(UUID doctorId) {
                return doctorRepository.findById(doctorId)
                                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        }

        @Override
        public java.util.List<Doctor> searchDoctors(String query) {
                return doctorRepository.searchDoctors(query);
        }
}
