package com.healthlink.domain.organization.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "service_offerings", indexes = {
        @Index(name = "idx_service_facility", columnList = "facility_id")
})
@Getter
@Setter
public class ServiceOffering extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "base_fee")
    private BigDecimal baseFee;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 15;

    @Column(name = "requires_staff_assignment", nullable = false)
    private Boolean requiresStaffAssignment = Boolean.FALSE;
}