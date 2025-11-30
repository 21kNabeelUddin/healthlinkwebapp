package com.healthlink.domain.record.repository;

import com.healthlink.domain.record.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    List<Prescription> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
    List<Prescription> findByAppointmentId(UUID appointmentId);
    List<Prescription> findByDoctorId(UUID doctorId);
}
