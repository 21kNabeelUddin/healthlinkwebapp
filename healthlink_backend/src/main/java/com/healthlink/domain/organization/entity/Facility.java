package com.healthlink.domain.organization.entity;

import com.healthlink.common.entity.BaseEntity;
import com.healthlink.domain.user.entity.Organization;
import com.healthlink.domain.user.entity.Doctor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "facilities", indexes = {
        @Index(name = "idx_facility_org", columnList = "organization_id"),
        @Index(name = "idx_facility_doctor", columnList = "doctor_owner_id")
})
@Getter
@Setter
public class Facility extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization; // nullable if doctor-owned

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_owner_id")
    private Doctor doctorOwner; // nullable if org-owned

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "requires_staff_assignment", nullable = false)
    private Boolean requiresStaffAssignment = Boolean.FALSE;
}