package com.healthlink.domain.appointment.repository;

import com.healthlink.domain.appointment.entity.Payment;
import com.healthlink.domain.appointment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByAppointmentDoctorIdAndStatus(UUID doctorId, PaymentStatus status);
    
    List<Payment> findByAppointmentPatientId(UUID patientId);

    List<Payment> findByAppointmentDoctorId(UUID doctorId);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM Payment p WHERE p.appointment.doctor.id = :doctorId AND p.status = :status")
    java.math.BigDecimal sumAmountByDoctorIdAndStatus(@org.springframework.data.repository.query.Param("doctorId") UUID doctorId, @org.springframework.data.repository.query.Param("status") PaymentStatus status);

    // Patient analytics
    @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM Payment p WHERE p.appointment.patient.id = :patientId AND p.status = 'COMPLETED'")
    java.math.BigDecimal sumAmountByPatientId(@org.springframework.data.repository.query.Param("patientId") UUID patientId);
}
