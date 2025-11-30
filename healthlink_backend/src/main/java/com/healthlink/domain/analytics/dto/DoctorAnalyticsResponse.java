package com.healthlink.domain.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAnalyticsResponse {
    private Integer totalAppointments;
    private Integer completedAppointments;
    private Integer pendingAppointments;
    private BigDecimal totalRevenue;
    private Integer totalPatients;
    private Double averageRating;
    private Integer reviewCount;
}
