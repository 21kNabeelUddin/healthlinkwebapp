package com.healthlink.domain.record.dto;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class PrescriptionResponse {
    UUID id;
    UUID patientId;
    UUID appointmentId;
    UUID doctorId;
    String title;
    String body;
    List<String> medications;
    List<String> interactionWarnings;

    OffsetDateTime createdAt;
}
