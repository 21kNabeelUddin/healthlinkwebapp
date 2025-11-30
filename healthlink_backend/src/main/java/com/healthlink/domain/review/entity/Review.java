package com.healthlink.domain.review.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Patient review of a doctor after completed appointment.
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
public class Review extends BaseEntity {

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Column(name = "rating", nullable = false)
    private int rating; // 1-5

    @Column(name = "comments", length = 2000)
    private String comments;

    @Column(name = "is_reported")
    private Boolean isReported = false;

    @Column(name = "report_reason", length = 500)
    private String reportReason;
}
