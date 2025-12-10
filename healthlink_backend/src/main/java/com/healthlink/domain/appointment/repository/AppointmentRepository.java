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

        // Native queries to filter out invalid statuses (PENDING_PAYMENT, CONFIRMED) at database level
        // This prevents Hibernate from trying to deserialize appointments with old enum values
        @Query(value = """
                SELECT * FROM appointments 
                WHERE doctor_id = :doctorId 
                AND status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')
                """, nativeQuery = true)
        List<Appointment> findByDoctorIdWithValidStatus(@Param("doctorId") UUID doctorId);

        @Query(value = """
                SELECT * FROM appointments 
                WHERE patient_id = :patientId 
                AND status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')
                """, nativeQuery = true)
        List<Appointment> findByPatientIdWithValidStatus(@Param("patientId") UUID patientId);

        default boolean staffHasConflictingAppointment(UUID staffId, LocalDateTime startTime,
                        LocalDateTime endTime, UUID excludeAppointmentId) {
                return staffHasConflictingAppointment(staffId, startTime, endTime, excludeAppointmentId,
                                AppointmentStatus.CANCELLED);
        }

        // Admin analytics - count appointments by status (only valid statuses)
        @Query(value = """
                SELECT COUNT(*) FROM appointments 
                WHERE status = :status
                AND status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')
                """, nativeQuery = true)
        Long countByStatus(@Param("status") String status);

        // Admin - get all appointments with valid statuses
        @Query(value = """
                SELECT * FROM appointments 
                WHERE deleted_at IS NULL
                AND status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')
                ORDER BY appointment_time DESC
                """, nativeQuery = true)
        java.util.List<Appointment> findAllWithValidStatus();

        List<Appointment> findByFacilityId(UUID facilityId);
}
