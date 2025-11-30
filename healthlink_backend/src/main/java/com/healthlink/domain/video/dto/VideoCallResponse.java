package com.healthlink.domain.video.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VideoCallResponse {
    private UUID id;
    private UUID appointmentId;
    private String sessionId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String recordingUrl;
    private UUID staffParticipantId;
    private LocalDateTime staffJoinedAt;
    private Boolean staffParticipantRequired;
}
