package com.healthlink.domain.appointment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AppointmentResponse {
    private UUID id;
    private String patientId;
    private String doctorId;
    private UUID facilityId;
    private UUID serviceOfferingId;
    private UUID assignedStaffId;
    private Boolean staffAssignmentRequired;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String type;
    private String notes;
    private LocalDateTime patientCheckInTime;
    private LocalDateTime staffCheckInTime;
    private BigDecimal fee;
    private Boolean isPaid;
}
