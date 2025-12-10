package com.healthlink.domain.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotResponse {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // AVAILABLE or BOOKED
}

