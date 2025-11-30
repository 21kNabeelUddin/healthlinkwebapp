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
public class PatientAnalyticsResponse {
    private Integer totalAppointments;
    private Integer completedAppointments;
    private Integer cancelledAppointments;
    private BigDecimal totalPayments;
    private Integer uniqueDoctorsVisited;
}
