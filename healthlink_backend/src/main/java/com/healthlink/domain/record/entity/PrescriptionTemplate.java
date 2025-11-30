package com.healthlink.domain.record.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "prescription_templates", indexes = {
        @Index(name = "idx_pretemplate_name", columnList = "name")
})
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PrescriptionTemplate extends BaseEntity {

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Lob
    @Column(name = "content", nullable = false)
    private String content; // Structured text with placeholders e.g. {{dosage}}, {{frequency}}

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
