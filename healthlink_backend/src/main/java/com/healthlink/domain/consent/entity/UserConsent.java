package com.healthlink.domain.consent.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_consents", indexes = {
        @Index(name = "idx_userconsent_user", columnList = "user_id"),
        @Index(name = "idx_userconsent_version", columnList = "consent_version")
})
@Getter
@Setter
public class UserConsent extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "consent_version", nullable = false, length = 50)
    private String consentVersion;

    @Column(name = "accepted_at", nullable = false)
    private OffsetDateTime acceptedAt = OffsetDateTime.now();
}