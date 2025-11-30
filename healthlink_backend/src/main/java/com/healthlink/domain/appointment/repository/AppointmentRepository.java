package com.healthlink.domain.appointment.repository;

import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

        List<Appointment> findByDoctorIdAndAppointmentTimeBetween(UUID doctorId, LocalDateTime start,
                        LocalDateTime end);

        List<Appointment> findByAppointmentTimeBetweenAndStatus(LocalDateTime start, LocalDateTime end,
                        AppointmentStatus status);

        List<Appointment> findByPatientId(UUID patientId);

        List<Appointment> findByDoctorIdAndStatus(UUID doctorId, AppointmentStatus status);

        boolean existsByDoctorIdAndPatientId(UUID doctorId, UUID patientId);

        @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId AND a.status != 'CANCELLED' AND ((a.appointmentTime < :endTime AND a.endTime > :startTime))")
        boolean existsOverlappingAppointment(@Param("doctorId") UUID doctorId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        @Query("""
                SELECT COUNT(a) > 0 FROM Appointment a
                WHERE a.assignedStaff.id = :staffId
                  AND (:excludeAppointmentId IS NULL OR a.id <> :excludeAppointmentId)
                  AND a.status <> :cancelledStatus
                  AND (a.appointmentTime < :endTime AND a.endTime > :startTime)
                """)
        boolean staffHasConflictingAppointment(@Param("staffId") UUID staffId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime,
                        @Param("excludeAppointmentId") UUID excludeAppointmentId,
                        @Param("cancelledStatus") AppointmentStatus cancelledStatus);

        Integer countByDoctorId(UUID doctorId);

        Integer countByDoctorIdAndStatus(UUID doctorId, AppointmentStatus status);

        @Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a WHERE a.doctor.id = :doctorId")
        Integer countDistinctPatientsByDoctorId(@Param("doctorId") UUID doctorId);

        // Patient analytics
        Integer countByPatientId(UUID patientId);

        Integer countByPatientIdAndStatus(UUID patientId, AppointmentStatus status);

        @Query("SELECT COUNT(DISTINCT a.doctor.id) FROM Appointment a WHERE a.patient.id = :patientId")
        Integer countDistinctDoctorsByPatientId(@Param("patientId") UUID patientId);

        // Organization-based analytics via Facility linking doctorOwner to organization
        @Query("SELECT COUNT(a) FROM Appointment a WHERE EXISTS (SELECT f.id FROM Facility f WHERE f.organization.id = :organizationId AND f.doctorOwner = a.doctor)")
        Integer countByOrganizationId(@Param("organizationId") UUID organizationId);

        @Query("SELECT COUNT(DISTINCT a.doctor.id) FROM Appointment a WHERE a.appointmentTime >= :after AND EXISTS (SELECT f.id FROM Facility f WHERE f.organization.id = :organizationId AND f.doctorOwner = a.doctor)")
        Integer countDistinctDoctorsByOrganizationIdAndAfter(@Param("organizationId") UUID organizationId,
                        @Param("after") LocalDateTime after);

        List<Appointment> findByDoctorId(UUID doctorId);

        default boolean staffHasConflictingAppointment(UUID staffId, LocalDateTime startTime,
                        LocalDateTime endTime, UUID excludeAppointmentId) {
                return staffHasConflictingAppointment(staffId, startTime, endTime, excludeAppointmentId,
                                AppointmentStatus.CANCELLED);
        }
}
