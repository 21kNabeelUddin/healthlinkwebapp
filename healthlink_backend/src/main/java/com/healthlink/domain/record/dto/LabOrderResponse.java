package com.healthlink.domain.record.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for exposing LabOrder information via API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabOrderResponse {
    private UUID id;
    private UUID patientId;
    private String orderName;
    private String description;
    private LocalDateTime orderedAt;
    private String resultUrl;
}
