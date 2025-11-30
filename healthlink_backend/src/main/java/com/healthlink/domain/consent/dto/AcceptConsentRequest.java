package com.healthlink.domain.consent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AcceptConsentRequest {
    @NotBlank
    private String version;
    @NotBlank
    private String language; // e.g. 'en' or 'ur'
}