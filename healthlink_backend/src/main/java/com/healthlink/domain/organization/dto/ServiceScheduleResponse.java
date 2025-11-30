package com.healthlink.domain.organization.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class ServiceScheduleResponse {
    private UUID id;
    private UUID serviceOfferingId;
    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}