package com.healthlink.domain.consent.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class UserConsentResponse {
    private UUID userId;
    private String consentVersion;
    private String language;
    private OffsetDateTime acceptedAt;
}