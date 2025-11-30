package com.healthlink.domain.consent.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConsentVersionResponse {
    private String version;
    private String language;
    private String content;
    private boolean active;
}