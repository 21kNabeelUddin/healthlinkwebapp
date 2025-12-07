package com.healthlink.domain.organization.entity;

import com.healthlink.common.entity.BaseEntity;
import com.healthlink.domain.user.entity.Organization;
import com.healthlink.domain.user.entity.Doctor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

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

    @Column(name = "town", length = 120)
    private String town;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "state", length = 120)
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "phone_number", length = 25)
    private String phoneNumber;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "opening_time", length = 10)
    private String openingTime;

    @Column(name = "closing_time", length = 10)
    private String closingTime;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "requires_staff_assignment", nullable = false)
    private Boolean requiresStaffAssignment = Boolean.FALSE;

    // Services offered: comma-separated values like "ONLINE,ONSITE" or just "ONSITE"
    @Column(name = "services_offered", length = 50)
    private String servicesOffered; // e.g., "ONLINE,ONSITE" or "ONSITE"
}