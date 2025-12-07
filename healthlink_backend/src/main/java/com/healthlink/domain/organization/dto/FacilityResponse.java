package com.healthlink.domain.organization.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class FacilityResponse {
    private UUID id;
    private String name;
    private String address;
    private String town;
    private String city;
    private String state;
    private String zipCode;
    private String phoneNumber;
    private String email;
    private String description;
    private String openingTime;
    private String closingTime;
    private Double latitude;
    private Double longitude;
    private BigDecimal consultationFee;
    private boolean active;
    private UUID organizationId;
    private UUID doctorOwnerId;
    private String servicesOffered; // e.g., "ONLINE,ONSITE" or "ONSITE"
}