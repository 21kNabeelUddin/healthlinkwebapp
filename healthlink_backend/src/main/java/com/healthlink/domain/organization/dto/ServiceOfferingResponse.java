package com.healthlink.domain.organization.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ServiceOfferingResponse {
    private UUID id;
    private UUID facilityId;
    private String name;
    private String description;
    private BigDecimal baseFee;
    private Integer durationMinutes;
    private Boolean requiresStaffAssignment;
}