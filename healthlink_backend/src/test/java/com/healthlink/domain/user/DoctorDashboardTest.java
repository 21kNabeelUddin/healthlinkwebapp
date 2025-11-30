package com.healthlink.domain.user;

import com.healthlink.domain.appointment.entity.PaymentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.appointment.repository.PaymentRepository;
import com.healthlink.domain.user.dto.DoctorDashboardDTO;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.repository.DoctorRepository;
import com.healthlink.domain.user.service.DoctorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorDashboardTest {

    @Mock private DoctorRepository doctorRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private AppointmentRepository appointmentRepository;

    private DoctorServiceImpl service;
    private final UUID doctorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DoctorServiceImpl(doctorRepository, paymentRepository, appointmentRepository);
    }

    @Test
    void shouldReturnDashboardData() {
        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setAverageRating(4.5);
        doctor.setTotalReviews(10);

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.VERIFIED))
                .thenReturn(new BigDecimal("5000.00"));
        when(paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.CAPTURED))
                .thenReturn(new BigDecimal("2000.00"));
        when(appointmentRepository.countByDoctorId(doctorId)).thenReturn(25);

        DoctorDashboardDTO dashboard = service.getDashboard(doctorId);

        assertNotNull(dashboard);
        assertEquals(new BigDecimal("7000.00"), dashboard.getTotalRevenue());
        assertEquals(25, dashboard.getTotalAppointments());
        assertEquals(4.5, dashboard.getAverageRating());
        assertEquals(10, dashboard.getTotalReviews());
    }
}
