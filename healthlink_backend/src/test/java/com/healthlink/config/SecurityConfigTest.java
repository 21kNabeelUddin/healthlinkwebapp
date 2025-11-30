package com.healthlink.config;

import com.healthlink.AbstractIntegrationTest;
import com.healthlink.security.jwt.JwtAuthenticationFilter;
import com.healthlink.security.rate.GlobalRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for SecurityConfig
 * Verifies security headers, CORS configuration, authentication rules, and
 * password encoding
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "healthlink.cors.allowed-origins=http://localhost:3000,http://testorigin.com",
                "healthlink.security.bcrypt-strength=10"
})
class SecurityConfigTest extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private SecurityConfig securityConfig;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private CorsConfigurationSource corsConfigurationSource;

        @Mock
        private JwtAuthenticationFilter jwtAuthFilter;

        @Mock
        private GlobalRateLimitFilter rateLimitFilter;

        @Mock
        private AuthenticationConfiguration authenticationConfiguration;

        /**
         * Test that security headers are properly configured in responses
         * Note: Even error responses should include security headers
         */
        @Test
        void securityFilterChain_shouldAddSecurityHeaders() throws Exception {
                // Using actuator/health which is a known valid endpoint
                mockMvc.perform(get("/actuator/health"))
                                .andExpect(header().exists("X-XSS-Protection"))
                                .andExpect(header().exists("Content-Security-Policy"))
                                .andExpect(header().string("Content-Security-Policy",
                                                "default-src 'self'; script-src 'self' 'unsafe-inline'; object-src 'none'; frame-ancestors 'none';"))
                                .andExpect(header().exists("Referrer-Policy"))
                                .andExpect(header().exists("X-Frame-Options"))
                                .andExpect(header().string("X-Frame-Options", "DENY"));
        }

        /**
         * Test that public endpoints are accessible without authentication
         * These endpoints should not return 401/403 (security block)
         */
        @Test
        void securityFilterChain_shouldAllowPublicEndpoints() throws Exception {
                // Actuator health should be accessible (security-wise)
                // Note: May return 503 (DOWN) due to RabbitMQ auth issues in test env,
                // but should NOT return 401/403 (security block)
                mockMvc.perform(get("/actuator/health"))
                                .andExpect(result -> {
                                        int status = result.getResponse().getStatus();
                                        // Not 401 or 403 means security allowed it through
                                        assertThat(status).isNotIn(401, 403);
                                });
        }

        /**
         * Test that protected endpoints require authentication
         */
        @Test
        void securityFilterChain_shouldProtectAuthenticatedEndpoints() throws Exception {
                mockMvc.perform(get("/api/v1/doctors/profile"))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(get("/api/v1/patients/appointments"))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(get("/api/v1/admin/users"))
                                .andExpect(status().isUnauthorized());
        }

        /**
         * Test CORS configuration source bean
         */
        @Test
        void corsConfigurationSource_shouldConfigureCorsCorrectly() {
                // Create a mock request with a path (required by UrlBasedCorsConfigurationSource)
                MockHttpServletRequest mockRequest = new MockHttpServletRequest();
                mockRequest.setServletPath("/api/v1/test");
                
                CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(mockRequest);

                assertThat(config).isNotNull();
                assertThat(config.getAllowedOrigins())
                                .containsExactlyInAnyOrder("http://localhost:3000", "http://testorigin.com");
                assertThat(config.getAllowedMethods())
                                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
                assertThat(config.getAllowedHeaders())
                                .containsExactlyInAnyOrder("Authorization", "Content-Type", "Accept",
                                                "X-Requested-With", "Cache-Control", "Pragma");
                assertThat(config.getAllowCredentials()).isTrue();
                assertThat(config.getMaxAge()).isEqualTo(3600L);
        }

        /**
         * Test that CORS does NOT allow wildcard headers
         */
        @Test
        void corsConfigurationSource_shouldNotAllowWildcardHeaders() {
                MockHttpServletRequest mockRequest = new MockHttpServletRequest();
                mockRequest.setServletPath("/api/v1/test");
                
                CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(mockRequest);

                assertThat(config.getAllowedHeaders()).doesNotContain("*");
                assertThat(config.getAllowedHeaders()).hasSize(6); // Explicit whitelist
        }

        /**
         * Test BCrypt password encoder bean
         */
        @Test
        void passwordEncoder_shouldUseBCryptWithConfigurableStrength() {
                assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);

                String rawPassword = "TestPassword123!";
                String encodedPassword = passwordEncoder.encode(rawPassword);

                assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
                assertThat(passwordEncoder.matches("WrongPassword", encodedPassword)).isFalse();
        }

        /**
         * Test that BCrypt strength is configurable
         */
        @Test
        void passwordEncoder_shouldUseConfiguredStrength() {
                // Test property sets strength to 10
                String password = "TestPassword123!";
                String encoded = passwordEncoder.encode(password);

                // BCrypt hashes start with $2a$<rounds>$
                assertThat(encoded).startsWith("$2a$10$");
        }

        /**
         * Test authentication manager bean creation
         */
        @Test
        void authenticationManager_shouldBeConfigured() throws Exception {
                SecurityConfig config = new SecurityConfig(jwtAuthFilter, rateLimitFilter);

                // Mock the authenticationConfiguration to return a non-null AuthenticationManager
                AuthenticationConfiguration mockAuthConfig = mock(AuthenticationConfiguration.class);
                AuthenticationManager mockAuthManager = mock(AuthenticationManager.class);
                when(mockAuthConfig.getAuthenticationManager()).thenReturn(mockAuthManager);

                assertThat(config.authenticationManager(mockAuthConfig)).isNotNull();
        }

        /**
         * Test CSRF is disabled for stateless API
         */
        @Test
        void securityFilterChain_shouldDisableCsrf() throws Exception {
                // CSRF should be disabled, so POST without CSRF token should not return 403 Forbidden
                // It may return other errors (400 bad request, 404 not found) but not CSRF block (403)
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType("application/json")
                                .content("{}"))
                                .andExpect(result -> {
                                        int status = result.getResponse().getStatus();
                                        // If CSRF were enabled, it would return 403
                                        // Not 403 means CSRF is disabled
                                        assertThat(status).isNotEqualTo(403);
                                });
        }

        /**
         * Test OPTIONS preflight requests are handled
         */
        @Test
        void securityFilterChain_shouldHandlePreflightRequests() throws Exception {
                mockMvc.perform(options("/api/v1/doctors/profile")
                                .header("Origin", "http://localhost:3000")
                                .header("Access-Control-Request-Method", "GET"))
                                .andExpect(status().isOk())
                                .andExpect(header().exists("Access-Control-Allow-Origin"))
                                .andExpect(header().exists("Access-Control-Allow-Methods"));
        }

        /**
         * Integration test: verify security filter chain builds without errors
         */
        @Test
        void securityFilterChain_shouldBuildSuccessfully() {
                assertThat(securityConfig).isNotNull();
                assertThat(corsConfigurationSource).isNotNull();
                assertThat(passwordEncoder).isNotNull();
        }

        /**
         * Test that session is stateless (no session is created for API requests)
         */
        @Test
        void securityFilterChain_shouldUseStatelessSession() throws Exception {
                // Verify that after a request, no session is created
                // The lack of Set-Cookie header for JSESSIONID indicates stateless session
                mockMvc.perform(get("/api/v1/auth/login"))
                                .andExpect(result -> {
                                        // Stateless sessions should not create a session
                                        assertThat(result.getRequest().getSession(false)).isNull();
                                });
        }
}
