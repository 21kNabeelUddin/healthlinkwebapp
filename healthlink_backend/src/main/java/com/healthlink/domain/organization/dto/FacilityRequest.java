package com.healthlink.domain.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FacilityRequest {
    @NotBlank
    private String name;

    @NotBlank
    @Size(max = 500)
    private String address;

    private String town;
    private String city;
    private String state;
    private String zipCode;

    @Size(max = 20)
    private String phoneNumber;

    @Email
    private String email;

    private String description;
    private String openingTime;
    private String closingTime;

    private Double latitude;
    private Double longitude;

    private BigDecimal consultationFee;

    // Services offered: comma-separated values like "ONLINE,ONSITE" or just "ONSITE"
    private String servicesOffered; // e.g., "ONLINE,ONSITE" or "ONSITE"
}