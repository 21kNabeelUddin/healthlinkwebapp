package com.healthlink.domain.video.service;

import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.video.dto.IceServerConfig;
import com.healthlink.domain.video.dto.InitiateCallRequest;
import com.healthlink.domain.video.dto.VideoCallResponse;
import com.healthlink.domain.video.dto.WebRTCTokenResponse;
import com.healthlink.domain.video.entity.VideoCall;
import com.healthlink.domain.video.repository.VideoCallRepository;
import com.healthlink.infrastructure.video.JanusService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VideoCallService {

    private final VideoCallRepository videoCallRepository;
    private final AppointmentRepository appointmentRepository;
    private final JanusService janusService;

    @Value("${healthlink.webrtc.signaling.url:ws://localhost:8080/signaling}")
    private String signalingUrl;

    @Value("${healthlink.jwt.secret:your-256-bit-secret-key-change-this-in-production-use-strong-random-key}")
    private String jwtSecret;

    @Value("${healthlink.webrtc.jwt.expiration:3600000}")
    private Long jwtExpiration; // 1 hour in milliseconds

    @Value("${healthlink.webrtc.ice-servers[0].urls:stun:stun.l.google.com:19302}")
    private String stunServer;

    @Value("${healthlink.webrtc.ice-servers[1].urls:turn:coturn:3478}")
    private String turnServer;

    @Value("${healthlink.webrtc.ice-servers[1].username:healthlink}")
    private String turnUsername;

    @Value("${healthlink.webrtc.ice-servers[1].credential:healthlink_turn_secret_2025}")
    private String turnCredential;

    public VideoCallResponse initiateCall(InitiateCallRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getPatientCheckInTime() == null) {
            throw new RuntimeException("Patient must check in before starting the call");
        }

        if (staffRequired(appointment) && appointment.getStaffCheckInTime() == null) {
            throw new RuntimeException("Assigned staff must check in before starting the call");
        }

        // Check if call already exists
        if (videoCallRepository.findByAppointmentId(appointment.getId()).isPresent()) {
            return mapToResponse(videoCallRepository.findByAppointmentId(appointment.getId()).get());
        }

        // Create Janus Session
        Long sessionId = janusService.createSession();
        Long handleId = janusService.attachPlugin(sessionId);

        // Create Room (using appointment ID hash as room ID for simplicity, or random)
        Long roomId = Math.abs(appointment.getId().getMostSignificantBits());
        String secret = UUID.randomUUID().toString();
        janusService.createRoom(sessionId, handleId, roomId, secret);

        VideoCall videoCall = new VideoCall();
        videoCall.setAppointment(appointment);
        videoCall.setAssignedStaff(appointment.getAssignedStaff());
        videoCall.setJanusSessionId(sessionId);
        videoCall.setJanusHandleId(handleId);
        videoCall.setRoomSecret(secret);
        videoCall.setStartedAt(LocalDateTime.now());
        videoCall.setStaffJoinedAt(appointment.getStaffCheckInTime());

        VideoCall savedCall = videoCallRepository.save(videoCall);
        return mapToResponse(savedCall);
    }

    public WebRTCTokenResponse getWebRTCToken(String sessionId, String userId) {
        // Generate JWT token for WebRTC signaling
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        String token = Jwts.builder()
                .subject(userId)
                .claim("sessionId", sessionId)
                .claim("type", "webrtc")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        // Build ICE servers configuration
        List<IceServerConfig> iceServers = new ArrayList<>();

        // Add STUN server (always available)
        iceServers.add(IceServerConfig.builder()
                .urls(List.of(stunServer, "stun:stun1.l.google.com:19302"))
                .build());

        // Add TURN server if configured
        if (turnServer != null && !turnServer.isEmpty()) {
            iceServers.add(IceServerConfig.builder()
                    .urls(List.of(turnServer))
                    .username(turnUsername)
                    .credential(turnCredential)
                    .build());
        }

        return WebRTCTokenResponse.builder()
                .token(token)
                .websocketUrl(signalingUrl)
                .expiresAt(expiryDate.toInstant().atZone(ZoneId.systemDefault()).toString())
                .iceServers(iceServers)
                .build();
    }

    private VideoCallResponse mapToResponse(VideoCall call) {
        // Derive status from VideoCall state
        String status;
        if (call.getEndedAt() != null) {
            status = "ENDED";
        } else if (call.getStartedAt() != null) {
            status = "ACTIVE";
        } else {
            status = "INITIATED";
        }

        return VideoCallResponse.builder()
                .id(call.getId())
                .appointmentId(call.getAppointment().getId())
                .sessionId(call.getJanusSessionId().toString())
                .status(status)
                .startTime(call.getStartedAt())
                .endTime(call.getEndedAt())
                .recordingUrl(call.getRecordingUrl())
                .staffParticipantId(call.getAssignedStaff() != null ? call.getAssignedStaff().getId() : null)
                .staffJoinedAt(call.getStaffJoinedAt())
                .staffParticipantRequired(staffRequired(call.getAppointment()))
                .build();
    }

    private boolean staffRequired(Appointment appointment) {
        if (appointment == null) {
            return false;
        }
        if (appointment.getServiceOffering() != null
                && Boolean.TRUE.equals(appointment.getServiceOffering().getRequiresStaffAssignment())) {
            return true;
        }
        return appointment.getFacility() != null
                && Boolean.TRUE.equals(appointment.getFacility().getRequiresStaffAssignment());
    }
}
