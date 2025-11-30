package com.healthlink.domain.review.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID doctorId,
        UUID patientId,
        UUID appointmentId,
        int rating,
        String comments,
        String reviewerName,
        Boolean isReported,
        String reportReason,
        String response, // Optional response from doctor/organization
        LocalDateTime respondedAt, // When doctor/org responded
        LocalDateTime createdAt) {
}
