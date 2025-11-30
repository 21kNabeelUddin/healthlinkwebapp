package com.healthlink.domain.organization.entity;

import com.healthlink.common.entity.BaseEntity;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payment_accounts", indexes = {
        @Index(name = "idx_payacct_doctor", columnList = "doctor_id"),
        @Index(name = "idx_payacct_org", columnList = "organization_id")
})
@Getter
@Setter
public class PaymentAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor; // present if doctor-level

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization; // present if centralized mapping

    @Column(name = "account_details", nullable = false, length = 500)
    private String accountDetails; // external payment identifier (NOT PHI)
}