package com.healthlink.domain.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FacilityRequest {
    @NotBlank
    private String name;
    private String address;
}