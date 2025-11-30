package com.healthlink.domain.consent.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "consent_versions")
@Getter
@Setter
public class ConsentVersion extends BaseEntity {
    @Column(name = "consent_version", nullable = false, length = 50, unique = true)
    private String consentVersion; // e.g. v1.0, v1.1

    @Column(name = "language", nullable = false, length = 10)
    private String language; // en, ur

    @Column(name = "content", nullable = false, length = 8000)
    private String content; // consent text (NO PHI)

    @Column(name = "active", nullable = false)
    private boolean active = true;
}