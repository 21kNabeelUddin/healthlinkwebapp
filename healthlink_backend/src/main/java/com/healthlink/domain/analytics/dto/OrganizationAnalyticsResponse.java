package com.healthlink.domain.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationAnalyticsResponse {
    private Integer totalDoctors;
    private Integer totalStaff;
    private Integer totalFacilities;
    private Integer totalServices;
    private Integer totalAppointments;
    private Integer activeDoctors; // Doctors with at least 1 appointment in last 30 days
}
