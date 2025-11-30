package com.healthlink.domain.video.controller;

import com.healthlink.domain.video.dto.InitiateCallRequest;
import com.healthlink.domain.video.dto.VideoCallResponse;
import com.healthlink.domain.video.dto.WebRTCTokenResponse;
import com.healthlink.domain.video.service.VideoCallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/video-calls")
@RequiredArgsConstructor
@Tag(name = "Video Calls", description = "WebRTC video consultation endpoints")
@SecurityRequirement(name = "bearerAuth")
public class VideoCallController {

    private final VideoCallService videoCallService;

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Initiate a video call")
    public ResponseEntity<VideoCallResponse> initiateCall(@Valid @RequestBody InitiateCallRequest request) {
        return ResponseEntity.ok(videoCallService.initiateCall(request));
    }

    @GetMapping("/webrtc-token")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'STAFF')")
    @Operation(summary = "Get WebRTC token with ICE server configuration")
    public ResponseEntity<WebRTCTokenResponse> getWebRTCToken(
            @RequestParam String sessionId,
            @RequestParam String userId) {
        return ResponseEntity.ok(videoCallService.getWebRTCToken(sessionId, userId));
    }
}
