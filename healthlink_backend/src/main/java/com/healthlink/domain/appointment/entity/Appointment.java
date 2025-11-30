package com.healthlink.domain.appointment.entity;

import com.healthlink.common.entity.BaseEntity;
import com.healthlink.domain.organization.entity.Facility;
import com.healthlink.domain.organization.entity.ServiceOffering;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appointment_doctor", columnList = "doctor_id"),
        @Index(name = "idx_appointment_patient", columnList = "patient_id"),
        @Index(name = "idx_appointment_time", columnList = "appointment_time"),
        @Index(name = "idx_appointment_status", columnList = "status")
})
@Getter
@Setter
public class Appointment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_offering_id")
    private ServiceOffering serviceOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private Staff assignedStaff;

    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status;

    @Column(name = "reason_for_visit")
    @Convert(converter = com.healthlink.security.encryption.FieldEncryptionConverter.class)
    private String reasonForVisit;

    @Column(name = "notes", length = 2000)
    @Convert(converter = com.healthlink.security.encryption.FieldEncryptionConverter.class)
    private String notes;

    @Column(name = "is_checked_in")
    private Boolean isCheckedIn = false;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    // Phase 2 additions: split patient vs staff check-in tracking
    @Column(name = "patient_check_in_time")
    private LocalDateTime patientCheckInTime;

    @Column(name = "staff_check_in_time")
    private LocalDateTime staffCheckInTime;

    @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;
}
