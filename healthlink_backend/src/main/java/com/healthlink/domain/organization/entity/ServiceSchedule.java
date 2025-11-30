package com.healthlink.domain.organization.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "service_schedules", indexes = {
        @Index(name = "idx_serviceschedule_offering", columnList = "service_offering_id")
})
@Getter
@Setter
public class ServiceSchedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_offering_id", nullable = false)
    private ServiceOffering serviceOffering;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek; // 1=Monday .. 7=Sunday

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}