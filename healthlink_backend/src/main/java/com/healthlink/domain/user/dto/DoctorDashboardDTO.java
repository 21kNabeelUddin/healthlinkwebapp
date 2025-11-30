package com.healthlink.domain.user.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DoctorDashboardDTO {
    private BigDecimal totalRevenue;
    private Integer totalAppointments;
    private Double averageRating;
    private Integer totalReviews;
}
