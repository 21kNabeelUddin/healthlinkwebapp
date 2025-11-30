package com.healthlink.domain.organization.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class FacilityResponse {
    private UUID id;
    private String name;
    private String address;
    private boolean active;
    private UUID organizationId;
    private UUID doctorOwnerId;
}