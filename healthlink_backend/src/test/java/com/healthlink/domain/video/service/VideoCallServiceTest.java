package com.healthlink.domain.video.service;

import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.organization.entity.Facility;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.entity.Staff;
import com.healthlink.domain.video.dto.InitiateCallRequest;
import com.healthlink.domain.video.dto.VideoCallResponse;
import com.healthlink.domain.video.dto.WebRTCTokenResponse;
import com.healthlink.domain.video.entity.VideoCall;
import com.healthlink.domain.video.repository.VideoCallRepository;
import com.healthlink.infrastructure.video.JanusService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VideoCallService
 * 
 * Tests cover:
 * - WebRTC token generation with JWT
 * - ICE server configuration (STUN + TURN)
 * - Video call initiation with Janus Gateway
 * - Staff check-in validation for 3-way calls
 * - Call state management
 * - Error scenarios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VideoCallService Tests")
class VideoCallServiceTest {

    @Mock
    private VideoCallRepository videoCallRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private JanusService janusService;

    @InjectMocks
    private VideoCallService videoCallService;

    private static final String JWT_SECRET = "test-secret-key-for-jwt-signing-minimum-256-bits-required-here-for-hs256-algorithm";
    private static final String SIGNALING_URL = "ws://localhost:8080/signaling";
    private static final String STUN_SERVER = "stun:stun.l.google.com:19302";
    private static final String TURN_SERVER = "turn:coturn:3478";
    private static final String TURN_USERNAME = "testuser";
    private static final String TURN_CREDENTIAL = "testcredential";

    private Appointment appointment;
    private Patient patient;
    private Doctor doctor;
    private Staff staff;

    @BeforeEach
    void setUp() {
        // Inject configuration values
        ReflectionTestUtils.setField(videoCallService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(videoCallService, "signalingUrl", SIGNALING_URL);
        ReflectionTestUtils.setField(videoCallService, "jwtExpiration", 3600000L);
        ReflectionTestUtils.setField(videoCallService, "stunServer", STUN_SERVER);
        ReflectionTestUtils.setField(videoCallService, "turnServer", TURN_SERVER);
        ReflectionTestUtils.setField(videoCallService, "turnUsername", TURN_USERNAME);
        ReflectionTestUtils.setField(videoCallService, "turnCredential", TURN_CREDENTIAL);

        // Setup test entities
        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setEmail("patient@test.com");
        patient.setFullName("Test Patient");

        doctor = new Doctor();
        doctor.setId(UUID.randomUUID());
        doctor.setEmail("doctor@test.com");
        doctor.setFullName("Dr. Test");

        staff = new Staff();
        staff.setId(UUID.randomUUID());
        staff.setEmail("staff@test.com");
        staff.setFullName("Test Staff");

        appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setPatientCheckInTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should generate valid WebRTC token with JWT")
    void shouldGenerateValidWebRTCToken() {
        // Given
        String sessionId = "test-session-123";
        String userId = "user-456";

        // When
        WebRTCTokenResponse response = videoCallService.getWebRTCToken(sessionId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getWebsocketUrl()).isEqualTo(SIGNALING_URL);
        assertThat(response.getExpiresAt()).isNotNull();

        // Verify JWT token contents
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(response.getToken())
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId);
        assertThat(claims.get("sessionId", String.class)).isEqualTo(sessionId);
        assertThat(claims.get("type", String.class)).isEqualTo("webrtc");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    @DisplayName("Should include STUN and TURN ICE servers in token response")
    void shouldIncludeIceServersInToken() {
        // Given
        String sessionId = "test-session-123";
        String userId = "user-456";

        // When
        WebRTCTokenResponse response = videoCallService.getWebRTCToken(sessionId, userId);

        // Then
        assertThat(response.getIceServers()).isNotEmpty();
        assertThat(response.getIceServers()).hasSize(2);

        // Verify STUN server
        assertThat(response.getIceServers().get(0).getUrls())
                .contains(STUN_SERVER)
                .contains("stun:stun1.l.google.com:19302");
        assertThat(response.getIceServers().get(0).getUsername()).isNull();
        assertThat(response.getIceServers().get(0).getCredential()).isNull();

        // Verify TURN server
        assertThat(response.getIceServers().get(1).getUrls()).contains(TURN_SERVER);
        assertThat(response.getIceServers().get(1).getUsername()).isEqualTo(TURN_USERNAME);
        assertThat(response.getIceServers().get(1).getCredential()).isEqualTo(TURN_CREDENTIAL);
    }

    @Test
    @DisplayName("Should initiate video call with Janus Gateway")
    void shouldInitiateVideoCallWithJanus() {
        // Given
        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(appointment.getId());

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(videoCallRepository.findByAppointmentId(appointment.getId())).thenReturn(Optional.empty());
        when(janusService.createSession()).thenReturn(12345L);
        when(janusService.attachPlugin(12345L)).thenReturn(67890L);
        doNothing().when(janusService).createRoom(anyLong(), anyLong(), anyLong(), anyString());

        VideoCall savedCall = new VideoCall();
        savedCall.setId(UUID.randomUUID());
        savedCall.setAppointment(appointment);
        savedCall.setJanusSessionId(12345L);
        savedCall.setJanusHandleId(67890L);
        savedCall.setStartedAt(LocalDateTime.now());

        when(videoCallRepository.save(any(VideoCall.class))).thenReturn(savedCall);

        // When
        VideoCallResponse response = videoCallService.initiateCall(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(savedCall.getId());
        assertThat(response.getAppointmentId()).isEqualTo(appointment.getId());
        assertThat(response.getSessionId()).isEqualTo("12345");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getStartTime()).isNotNull();

        verify(janusService).createSession();
        verify(janusService).attachPlugin(12345L);
        verify(janusService).createRoom(eq(12345L), eq(67890L), anyLong(), anyString());
        verify(videoCallRepository).save(any(VideoCall.class));
    }

    @Test
    @DisplayName("Should throw exception when patient not checked in")
    void shouldThrowExceptionWhenPatientNotCheckedIn() {
        // Given
        appointment.setPatientCheckInTime(null);
        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(appointment.getId());

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        // When / Then
        assertThatThrownBy(() -> videoCallService.initiateCall(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Patient must check in before starting the call");

        verify(janusService, never()).createSession();
        verify(videoCallRepository, never()).save(any(VideoCall.class));
    }

    @Test
    @DisplayName("Should throw exception when staff not checked in for staff-required appointments")
    void shouldThrowExceptionWhenStaffNotCheckedIn() {
        // Given
        Facility facility = new Facility();
        facility.setId(UUID.randomUUID());
        facility.setRequiresStaffAssignment(true);
        
        appointment.setFacility(facility);
        appointment.setAssignedStaff(staff);
        appointment.setStaffCheckInTime(null);
        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(appointment.getId());

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        // When / Then
        assertThatThrownBy(() -> videoCallService.initiateCall(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Assigned staff must check in before starting the call");

        verify(janusService, never()).createSession();
        verify(videoCallRepository, never()).save(any(VideoCall.class));
    }

    @Test
    @DisplayName("Should return existing call if already initiated")
    void shouldReturnExistingCallIfAlreadyInitiated() {
        // Given
        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(appointment.getId());

        VideoCall existingCall = new VideoCall();
        existingCall.setId(UUID.randomUUID());
        existingCall.setAppointment(appointment);
        existingCall.setJanusSessionId(12345L);
        existingCall.setJanusHandleId(67890L);
        existingCall.setStartedAt(LocalDateTime.now());

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(videoCallRepository.findByAppointmentId(appointment.getId())).thenReturn(Optional.of(existingCall));

        // When
        VideoCallResponse response = videoCallService.initiateCall(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(existingCall.getId());
        assertThat(response.getStatus()).isEqualTo("ACTIVE");

        verify(janusService, never()).createSession();
        verify(videoCallRepository, never()).save(any(VideoCall.class));
    }

    @Test
    @DisplayName("Should throw exception when appointment not found")
    void shouldThrowExceptionWhenAppointmentNotFound() {
        // Given
        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(UUID.randomUUID());

        when(appointmentRepository.findById(any())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> videoCallService.initiateCall(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Appointment not found");

        verify(janusService, never()).createSession();
    }

    @Test
    @DisplayName("Should allow staff-required call when staff checked in")
    void shouldAllowStaffRequiredCallWhenStaffCheckedIn() {
        // Given
        Facility facility = new Facility();
        facility.setId(UUID.randomUUID());
        facility.setRequiresStaffAssignment(true);
        
        appointment.setFacility(facility);
        appointment.setAssignedStaff(staff);
        appointment.setStaffCheckInTime(LocalDateTime.now());
        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(appointment.getId());

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(videoCallRepository.findByAppointmentId(appointment.getId())).thenReturn(Optional.empty());
        when(janusService.createSession()).thenReturn(12345L);
        when(janusService.attachPlugin(12345L)).thenReturn(67890L);

        VideoCall savedCall = new VideoCall();
        savedCall.setId(UUID.randomUUID());
        savedCall.setAppointment(appointment);
        savedCall.setAssignedStaff(staff);
        savedCall.setJanusSessionId(12345L);
        savedCall.setJanusHandleId(67890L);
        savedCall.setStartedAt(LocalDateTime.now());
        savedCall.setStaffJoinedAt(appointment.getStaffCheckInTime());

        when(videoCallRepository.save(any(VideoCall.class))).thenReturn(savedCall);

        // When
        VideoCallResponse response = videoCallService.initiateCall(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStaffParticipantId()).isEqualTo(staff.getId());
        assertThat(response.getStaffJoinedAt()).isNotNull();
        assertThat(response.getStaffParticipantRequired()).isTrue();

        verify(videoCallRepository).save(argThat(call ->
                call.getAssignedStaff() != null && call.getStaffJoinedAt() != null
        ));
    }

    @Test
    @DisplayName("Should map VideoCall to VideoCallResponse with ENDED status")
    void shouldMapVideoCallWithEndedStatus() {
        // Given
        appointment.setAssignedStaff(staff);
        appointment.setStaffCheckInTime(LocalDateTime.now());
        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(appointment.getId());

        VideoCall existingCall = new VideoCall();
        existingCall.setId(UUID.randomUUID());
        existingCall.setAppointment(appointment);
        existingCall.setAssignedStaff(staff);
        existingCall.setJanusSessionId(12345L);
        existingCall.setStartedAt(LocalDateTime.now().minusMinutes(30));
        existingCall.setEndedAt(LocalDateTime.now());
        existingCall.setStaffJoinedAt(appointment.getStaffCheckInTime());

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(videoCallRepository.findByAppointmentId(appointment.getId())).thenReturn(Optional.of(existingCall));

        // When
        VideoCallResponse response = videoCallService.initiateCall(request);

        // Then
        assertThat(response.getStatus()).isEqualTo("ENDED");
        assertThat(response.getEndTime()).isNotNull();
    }

    @Test
    @DisplayName("Should generate unique JWT tokens for different users")
    void shouldGenerateUniqueJwtTokens() {
        // Given
        String sessionId = "test-session-123";
        String userId1 = "user-123";
        String userId2 = "user-456";

        // When
        WebRTCTokenResponse response1 = videoCallService.getWebRTCToken(sessionId, userId1);
        WebRTCTokenResponse response2 = videoCallService.getWebRTCToken(sessionId, userId2);

        // Then
        assertThat(response1.getToken()).isNotEqualTo(response2.getToken());

        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims1 = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(response1.getToken()).getPayload();
        Claims claims2 = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(response2.getToken()).getPayload();

        assertThat(claims1.getSubject()).isEqualTo(userId1);
        assertThat(claims2.getSubject()).isEqualTo(userId2);
    }

    @Test
    @DisplayName("Should include token expiration in response")
    void shouldIncludeTokenExpiration() {
        // Given
        String sessionId = "test-session-123";
        String userId = "user-456";

        // When
        WebRTCTokenResponse response = videoCallService.getWebRTCToken(sessionId, userId);

        // Then
        assertThat(response.getExpiresAt()).isNotNull();

        // Verify expiration is approximately 1 hour from now (allow 5 second variance)
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(response.getToken()).getPayload();

        long expirationTimeMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(expirationTimeMs).isBetween(3595000L, 3605000L); // 1 hour ± 5 seconds
    }
}
