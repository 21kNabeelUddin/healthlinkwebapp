package com.healthlink.domain.video.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlink.AbstractIntegrationTest;
import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.consent.entity.ConsentVersion;
import com.healthlink.domain.consent.entity.UserConsent;
import com.healthlink.domain.consent.repository.ConsentVersionRepository;
import com.healthlink.domain.consent.repository.UserConsentRepository;
import com.healthlink.domain.organization.entity.Facility;
import com.healthlink.domain.organization.repository.FacilityRepository;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.entity.Staff;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.domain.video.dto.InitiateCallRequest;
import com.healthlink.domain.video.repository.VideoCallRepository;
import com.healthlink.infrastructure.video.JanusService;
import com.healthlink.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for VideoCallController
 * 
 * Tests cover:
 * - WebRTC token endpoint with authentication
 * - JWT token structure and claims validation
 * - ICE servers configuration in response
 * - Video call initiation with RBAC
 * - Error scenarios (401, 403, 404, 400)
 */
@AutoConfigureMockMvc
@DisplayName("VideoCallController Integration Tests")
class VideoCallControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private VideoCallRepository videoCallRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Autowired
    private ConsentVersionRepository consentVersionRepository;

    @Autowired
    private UserConsentRepository userConsentRepository;

    @MockitoBean
    private JanusService janusService;

    @Value("${healthlink.jwt.secret}")
    private String jwtSecret;

    private Doctor doctor;
    private Patient patient;
    private Staff staff;
    private Appointment appointment;
    private String doctorToken;
    private String patientToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        // Clear repositories
        videoCallRepository.deleteAll();
        appointmentRepository.deleteAll();
        userConsentRepository.deleteAll();
        userRepository.deleteAll();
        facilityRepository.deleteAll();
        consentVersionRepository.deleteAll();

        // Create consent version first (required by ConsentEnforcementFilter)
        ConsentVersion consentVersion = new ConsentVersion();
        consentVersion.setConsentVersion("v1.0");
        consentVersion.setLanguage("en");
        consentVersion.setContent("Test consent content for video calls");
        consentVersion.setActive(true);
        consentVersionRepository.save(consentVersion);

        // Create test doctor
        doctor = new Doctor();
        doctor.setEmail("doctor-webrtc@test.com");
        doctor.setPasswordHash(passwordEncoder.encode("password123"));
        doctor.setFullName("Dr. WebRTC Test");
        doctor.setRole(UserRole.DOCTOR);
        doctor.setApprovalStatus(ApprovalStatus.APPROVED);
        doctor.setIsActive(true);
        doctor.setIsEmailVerified(true); // Required for isEnabled() to return true
        doctor.setSpecialization("Cardiology");
        doctor.setPmdcId("12345-P"); // Required field for Doctor entity
        doctor = (Doctor) userRepository.save(doctor);

        // Create test patient
        patient = new Patient();
        patient.setEmail("patient-webrtc@test.com");
        patient.setPasswordHash(passwordEncoder.encode("password123"));
        patient.setFullName("Patient WebRTC Test");
        patient.setRole(UserRole.PATIENT);
        patient.setApprovalStatus(ApprovalStatus.APPROVED);
        patient.setIsActive(true);
        patient.setIsEmailVerified(true); // Required for isEnabled() to return true
        patient = (Patient) userRepository.save(patient);

        // Create test staff
        staff = new Staff();
        staff.setEmail("staff-webrtc@test.com");
        staff.setPasswordHash(passwordEncoder.encode("password123"));
        staff.setFullName("Staff WebRTC Test");
        staff.setRole(UserRole.STAFF);
        staff.setApprovalStatus(ApprovalStatus.APPROVED);
        staff.setIsActive(true);
        staff.setIsEmailVerified(true); // Required for isEnabled() to return true
        staff = (Staff) userRepository.save(staff);

        // Create user consents for all users (required by ConsentEnforcementFilter)
        UserConsent doctorConsent = new UserConsent();
        doctorConsent.setUserId(doctor.getId());
        doctorConsent.setConsentVersion("v1.0");
        userConsentRepository.save(doctorConsent);

        UserConsent patientConsent = new UserConsent();
        patientConsent.setUserId(patient.getId());
        patientConsent.setConsentVersion("v1.0");
        userConsentRepository.save(patientConsent);

        UserConsent staffConsent = new UserConsent();
        staffConsent.setUserId(staff.getId());
        staffConsent.setConsentVersion("v1.0");
        userConsentRepository.save(staffConsent);

        // Create test appointment
        appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentTime(LocalDateTime.now());
        appointment.setEndTime(LocalDateTime.now().plusHours(1));
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setPatientCheckInTime(LocalDateTime.now()); // Patient checked in
        appointment = appointmentRepository.save(appointment);

        // Generate JWT tokens using UserDetailsService (like SecurityIntegrationTest does)
        UserDetails doctorDetails = userDetailsService.loadUserByUsername(doctor.getEmail());
        UserDetails patientDetails = userDetailsService.loadUserByUsername(patient.getEmail());
        UserDetails staffDetails = userDetailsService.loadUserByUsername(staff.getEmail());

        doctorToken = jwtService.generateAccessToken(doctorDetails, doctor.getId(), doctor.getRole().name());
        patientToken = jwtService.generateAccessToken(patientDetails, patient.getId(), patient.getRole().name());
        staffToken = jwtService.generateAccessToken(staffDetails, staff.getId(), staff.getRole().name());

        // Mock Janus service
        when(janusService.createSession()).thenReturn(12345L);
        when(janusService.attachPlugin(anyLong())).thenReturn(67890L);
        doNothing().when(janusService).createRoom(anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Should get WebRTC token with valid JWT and ICE servers")
    void shouldGetWebRTCTokenWithValidJWT() throws Exception {
        // Given
        String sessionId = "test-session-123";
        String userId = patient.getId().toString();

        // When - API returns wrapped response: {"data": {...}, "meta": {...}}
        MvcResult result = mockMvc.perform(get("/api/v1/video-calls/webrtc-token")
                        .header("Authorization", "Bearer " + patientToken)
                        .param("sessionId", sessionId)
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.websocketUrl").exists())
                .andExpect(jsonPath("$.data.expiresAt").exists())
                .andExpect(jsonPath("$.data.iceServers").isArray())
                .andExpect(jsonPath("$.data.iceServers[0].urls").isArray())
                .andReturn();

        // Then - Verify JWT token structure
        String responseBody = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        String webrtcToken = (String) data.get("token");

        assertThat(webrtcToken).isNotNull().isNotEmpty();

        // Verify JWT claims
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(webrtcToken)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId);
        assertThat(claims.get("sessionId", String.class)).isEqualTo(sessionId);
        assertThat(claims.get("type", String.class)).isEqualTo("webrtc");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();

        // Verify ICE servers
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> iceServers = (List<Map<String, Object>>) data.get("iceServers");
        assertThat(iceServers).hasSizeGreaterThanOrEqualTo(1);

        // STUN server should be first
        Map<String, Object> stunServer = iceServers.get(0);
        @SuppressWarnings("unchecked")
        List<String> stunUrls = (List<String>) stunServer.get("urls");
        assertThat(stunUrls).contains("stun:stun.l.google.com:19302");

        // TURN server should exist (if configured)
        if (iceServers.size() > 1) {
            Map<String, Object> turnServer = iceServers.get(1);
            @SuppressWarnings("unchecked")
            List<String> turnUrls = (List<String>) turnServer.get("urls");
            assertThat(turnUrls.get(0)).startsWith("turn:");
            assertThat(turnServer.get("username")).isNotNull();
            assertThat(turnServer.get("credential")).isNotNull();
        }
    }

    @Test
    @DisplayName("Should return 401 when getting WebRTC token without authentication")
    void shouldReturn401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/video-calls/webrtc-token")
                        .param("sessionId", "test-session")
                        .param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow doctor to get WebRTC token")
    void shouldAllowDoctorToGetWebRTCToken() throws Exception {
        mockMvc.perform(get("/api/v1/video-calls/webrtc-token")
                        .header("Authorization", "Bearer " + doctorToken)
                        .param("sessionId", "test-session")
                        .param("userId", doctor.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow staff to get WebRTC token")
    void shouldAllowStaffToGetWebRTCToken() throws Exception {
        mockMvc.perform(get("/api/v1/video-calls/webrtc-token")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("sessionId", "test-session")
                        .param("userId", staff.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should initiate video call when doctor authenticated")
    void shouldInitiateVideoCallWhenDoctorAuthenticated() throws Exception {
        // Given
        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(appointment.getId());

        // When - API returns wrapped response: {"data": {...}, "meta": {...}}
        mockMvc.perform(post("/api/v1/video-calls/initiate")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.appointmentId").value(appointment.getId().toString()))
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // Then
        verify(janusService).createSession();
        verify(janusService).attachPlugin(12345L);
        verify(janusService).createRoom(anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Should return 403 when patient tries to initiate call")
    void shouldReturn403WhenPatientTriesToInitiateCall() throws Exception {
        // Given
        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(appointment.getId());

        // When
        mockMvc.perform(post("/api/v1/video-calls/initiate")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Then
        verify(janusService, never()).createSession();
    }

    @Test
    @DisplayName("Should return 401 when initiating call without authentication")
    void shouldReturn401WhenInitiatingCallWithoutAuthentication() throws Exception {
        // Given
        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(appointment.getId());

        // When
        mockMvc.perform(post("/api/v1/video-calls/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Then
        verify(janusService, never()).createSession();
    }

    @Test
    @DisplayName("Should return 500 when appointment not found")
    void shouldReturn500WhenAppointmentNotFound() throws Exception {
        // Given
        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(UUID.randomUUID()); // Non-existent appointment

        // When
        mockMvc.perform(post("/api/v1/video-calls/initiate")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError()); // Service throws RuntimeException, handled as 500

        // Then
        verify(janusService, never()).createSession();
    }

    @Test
    @DisplayName("Should return 500 when patient not checked in")
    void shouldReturn500WhenPatientNotCheckedIn() throws Exception {
        // Given
        appointment.setPatientCheckInTime(null); // Patient not checked in
        appointmentRepository.save(appointment);

        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(appointment.getId());

        // When
        mockMvc.perform(post("/api/v1/video-calls/initiate")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        // Then
        verify(janusService, never()).createSession();
    }

    @Test
    @DisplayName("Should return 500 when staff not checked in for staff-required appointment")
    void shouldReturn500WhenStaffNotCheckedInForStaffRequiredAppointment() throws Exception {
        // Given
        Facility facility = new Facility();
        facility.setName("Test Facility");
        facility.setAddress("Test Address");
        facility.setRequiresStaffAssignment(true); // Staff required
        facility = facilityRepository.save(facility);

        appointment.setFacility(facility);
        appointment.setAssignedStaff(staff);
        appointment.setStaffCheckInTime(null); // Staff not checked in
        appointmentRepository.save(appointment);

        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(appointment.getId());

        // When
        mockMvc.perform(post("/api/v1/video-calls/initiate")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        // Then
        verify(janusService, never()).createSession();
    }

    @Test
    @DisplayName("Should successfully initiate call when staff checked in for staff-required appointment")
    void shouldSuccessfullyInitiateCallWhenStaffCheckedInForStaffRequiredAppointment() throws Exception {
        // Given
        Facility facility = new Facility();
        facility.setName("Test Facility");
        facility.setAddress("Test Address");
        facility.setRequiresStaffAssignment(true); // Staff required
        facility = facilityRepository.save(facility);

        appointment.setFacility(facility);
        appointment.setAssignedStaff(staff);
        appointment.setStaffCheckInTime(LocalDateTime.now()); // Staff checked in
        appointmentRepository.save(appointment);

        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(appointment.getId());

        // When - API returns wrapped response: {"data": {...}, "meta": {...}}
        mockMvc.perform(post("/api/v1/video-calls/initiate")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staffParticipantId").value(staff.getId().toString()))
                .andExpect(jsonPath("$.data.staffParticipantRequired").value(true))
                .andExpect(jsonPath("$.data.staffJoinedAt").exists());

        // Then
        verify(janusService).createSession();
        verify(janusService).attachPlugin(12345L);
        verify(janusService).createRoom(anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Should return existing call if already initiated")
    void shouldReturnExistingCallIfAlreadyInitiated() throws Exception {
        // Given - First initiate a call
        InitiateCallRequest request = new InitiateCallRequest();
        request.setAppointmentId(appointment.getId());

        mockMvc.perform(post("/api/v1/video-calls/initiate")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Reset mock to verify it's not called again
        reset(janusService);

        // When - Try to initiate the same call again (API returns wrapped response)
        mockMvc.perform(post("/api/v1/video-calls/initiate")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentId").value(appointment.getId().toString()));

        // Then - Janus service should not be called again
        verify(janusService, never()).createSession();
        verify(janusService, never()).attachPlugin(anyLong());
        verify(janusService, never()).createRoom(anyLong(), anyLong(), anyLong(), anyString());
    }
}
