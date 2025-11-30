package com.healthlink.security.jwt;

import com.healthlink.AbstractIntegrationTest;
import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
                "healthlink.jwt.secret=integration-test-secret-key-which-is-long-enough-256bits!",
                "healthlink.jwt.access-token-expiration=900000",
                "healthlink.jwt.refresh-token-expiration=604800000",
                "healthlink.phi.encryption-key=12345678901234567890123456789012EXTRA",
                "spring.main.allow-bean-definition-overriding=true"
})
@org.springframework.test.annotation.DirtiesContext
class LogoutIntegrationTest extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private JwtService jwtService;
        @Autowired
        private UserRepository userRepository;

        private User user;
        private String accessToken;
        private String refreshToken;

        @BeforeEach
        void setup() {
                userRepository.deleteAll();

                // Use concrete Patient entity instead of anonymous inner class for Hibernate
                Patient patient = new Patient();
                patient.setEmail("logout.int@test.com");
                patient.setFirstName("First");
                patient.setLastName("Last");
                patient.setRole(UserRole.PATIENT);
                patient.setIsActive(true);
                patient.setIsEmailVerified(true);
                user = userRepository.save(patient);

                var ud = org.springframework.security.core.userdetails.User
                                .withUsername(user.getEmail())
                                .password("nopass")
                                .authorities("ROLE_PATIENT")
                                .build();
                accessToken = jwtService.generateAccessToken(ud, user.getId(), user.getRole().name());
                refreshToken = jwtService.generateRefreshToken(ud, user.getId());
        }

        @Test
        void logoutBlacklistsAccessTokenAndRevokesRefresh() throws Exception {
                mockMvc.perform(post("/api/v1/auth/logout")
                                .header("Authorization", "Bearer " + accessToken)
                                .header("X-Refresh-Token", refreshToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNoContent());
        }
}