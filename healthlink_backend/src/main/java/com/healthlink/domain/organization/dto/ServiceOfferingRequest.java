package com.healthlink.domain.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ServiceOfferingRequest {
    @NotNull
    private java.util.UUID facilityId;
    @NotBlank
    private String name;
    private String description;
    private BigDecimal baseFee;
    @Min(5)
    @Max(180)
    private Integer durationMinutes = 15;
    private Boolean requiresStaffAssignment = Boolean.FALSE;
}