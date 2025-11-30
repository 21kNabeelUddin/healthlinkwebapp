package com.healthlink.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlink.AbstractIntegrationTest;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.security.jwt.JwtService;
import com.healthlink.security.token.AccessTokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security Integration Tests
 * Tests end-to-end security flows including authentication, authorization, and
 * token management
 */
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private AccessTokenBlacklistService blacklistService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserDetailsService userDetailsService;

        private static final String STRONG_PASSWORD = "Password123!";

        @Test
        void accessPublicEndpoint_shouldSucceed() throws Exception {
                mockMvc.perform(get("/api/health"))
                                .andExpect(status().isOk());
        }

        @Test
        void accessProtectedEndpoint_withoutAuth_shouldReturn401() throws Exception {
                mockMvc.perform(get("/api/v1/consent/active"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        void accessProtectedEndpoint_withAuth_shouldSucceed() throws Exception {
                mockMvc.perform(get("/api/v1/consent/active"))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        void accessAdminEndpoint_asPatient_shouldReturn403() throws Exception {
                mockMvc.perform(delete("/api/v1/admin/cache/all"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void accessAdminEndpoint_asAdmin_shouldSucceed() throws Exception {
                mockMvc.perform(delete("/api/v1/admin/cache/all"))
                                .andExpect(status().isOk());
        }

        @Test
        void login_withValidCredentials_shouldReturnTokens() throws Exception {
                String email = randomEmail();
                createApprovedDoctor(email, STRONG_PASSWORD);

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buildLoginPayload(email, STRONG_PASSWORD)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
        }

        @Test
        void login_withInvalidCredentials_shouldReturn401() throws Exception {
                String email = randomEmail();
                createApprovedDoctor(email, STRONG_PASSWORD);

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buildLoginPayload(email, "WrongPass123!")))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void accessWithBlacklistedToken_shouldReturn401() throws Exception {
                Doctor doctor = createApprovedDoctor(randomEmail(), STRONG_PASSWORD);
                UserDetails userDetails = userDetailsService.loadUserByUsername(doctor.getEmail());
                String token = jwtService.generateAccessToken(userDetails, doctor.getId(), doctor.getRole().name());
                String jti = jwtService.extractJti(token);

                // Blacklist the token
                blacklistService.blacklist(jti);

                mockMvc.perform(get("/api/v1/consent/active")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void refreshToken_withValidToken_shouldReturnNewTokens() throws Exception {
                String email = randomEmail();
                createApprovedDoctor(email, STRONG_PASSWORD);
                JsonNode loginResponse = performLogin(email, STRONG_PASSWORD);
                String refreshToken = loginResponse.path("data").path("refreshToken").asText();

                mockMvc.perform(post("/api/v1/auth/refresh")
                                .header("Authorization", "Bearer " + refreshToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
        }

        @Test
        void logout_shouldBlacklistToken() throws Exception {
                String email = randomEmail();
                createApprovedDoctor(email, STRONG_PASSWORD);
                JsonNode loginResponse = performLogin(email, STRONG_PASSWORD);
                String accessToken = loginResponse.path("data").path("accessToken").asText();
                String refreshToken = loginResponse.path("data").path("refreshToken").asText();

                mockMvc.perform(post("/api/v1/auth/logout")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "refreshToken": "%s"
                                                }
                                                """.formatted(refreshToken)))
                                .andExpect(status().isNoContent());

                // Verify token is blacklisted
                String jti = jwtService.extractJti(accessToken);
                assertTrue(blacklistService.isBlacklisted(jti));
        }

        @Test
        void corsHeaders_shouldBePresent() throws Exception {
                mockMvc.perform(options("/api/v1/consent/active")
                                .header("Origin", "http://localhost:3000")
                                .header("Access-Control-Request-Method", "GET"))
                                .andExpect(status().isOk())
                                .andExpect(header().exists("Access-Control-Allow-Origin"));
        }

        private String buildLoginPayload(String email, String password) {
                return """
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password);
        }

        private String randomEmail() {
                return "user-" + UUID.randomUUID() + "@healthlink.test";
        }

        private JsonNode performLogin(String email, String password) throws Exception {
                String response = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(buildLoginPayload(email, password)))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();
                return objectMapper.readTree(response);
        }

        private Doctor createApprovedDoctor(String email, String rawPassword) {
                Doctor doctor = new Doctor();
                doctor.setEmail(email);
                doctor.setFirstName("Doctor");
                doctor.setLastName("User");
                doctor.setRole(UserRole.DOCTOR);
                doctor.setApprovalStatus(ApprovalStatus.APPROVED);
                doctor.setIsEmailVerified(true);
                doctor.setIsActive(true);
                doctor.setPreferredLanguage("en");
                doctor.setPmdcId(generatePmdcId());
                doctor.setSpecialization("Cardiology");
                doctor.setPmdcVerified(true);
                doctor.setAllowFullRefundOnDoctorCancellation(true);
                doctor.setPasswordHash(passwordEncoder.encode(rawPassword));
                return userRepository.save(doctor);
        }

        private String generatePmdcId() {
                int value = (int) (Math.random() * 90000) + 10000;
                return String.format("%05d-P", value);
        }
}
