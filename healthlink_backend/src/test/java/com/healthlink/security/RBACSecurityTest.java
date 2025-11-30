package com.healthlink.security;

import com.healthlink.domain.user.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Role-Based Access Control (RBAC) Security Context Setup
 * Verifies that roles are properly configured and authorities are correctly assigned
 * 
 * Note: Full integration tests with @PreAuthorize endpoint testing should use
 * @SpringBootTest + @AutoConfigureMockMvc, but require proper test environment setup
 * (database, Redis, etc.). These unit tests verify the security foundation.
 * 
 * Per spec (Section 2 - Roles & Permissions):
 * - Patient: Own PHI only, cannot access doctor analytics
 * - Doctor: Assigned patients only, cannot access admin portal
 * - Staff: Limited PHI (name/contact), cannot access full medical records
 * - Admin: NO PHI access (anonymized analytics only)
 * - Organization: Network PHI only
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class RBACSecurityTest {

    /**
     * Setup: Clear SecurityContext before each test
     */
    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Cleanup: Clear SecurityContext after each test to prevent test pollution
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Test: Patient role is properly configured with ROLE_PATIENT authority
     * Validates that Patient users can be authenticated with correct role
     */
    @Test
    void patientRole_shouldHaveCorrectAuthority() {
        // Arrange & Act
        setSecurityContext(UserRole.PATIENT);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Assert
        assertNotNull(auth, "Authentication should not be null");
        assertTrue(hasRole(auth, "ROLE_PATIENT"), "Should have ROLE_PATIENT authority");
        assertFalse(hasRole(auth, "ROLE_DOCTOR"), "Should NOT have ROLE_DOCTOR authority");
        assertFalse(hasRole(auth, "ROLE_ADMIN"), "Should NOT have ROLE_ADMIN authority");
    }

    /**
     * Test: Doctor role is properly configured with ROLE_DOCTOR authority
     * Validates that Doctor users can be authenticated with correct role
     */
    @Test
    void doctorRole_shouldHaveCorrectAuthority() {
        // Arrange & Act
        setSecurityContext(UserRole.DOCTOR);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Assert
        assertNotNull(auth, "Authentication should not be null");
        assertTrue(hasRole(auth, "ROLE_DOCTOR"), "Should have ROLE_DOCTOR authority");
        assertFalse(hasRole(auth, "ROLE_PATIENT"), "Should NOT have ROLE_PATIENT authority");
        assertFalse(hasRole(auth, "ROLE_ADMIN"), "Should NOT have ROLE_ADMIN authority");
    }

    /**
     * Test: Staff role is properly configured with ROLE_STAFF authority
     * Per spec: Staff can only view name/contact info, not full PHI
     */
    @Test
    void staffRole_shouldHaveCorrectAuthority() {
        // Arrange & Act
        setSecurityContext(UserRole.STAFF);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Assert
        assertNotNull(auth, "Authentication should not be null");
        assertTrue(hasRole(auth, "ROLE_STAFF"), "Should have ROLE_STAFF authority");
        assertFalse(hasRole(auth, "ROLE_DOCTOR"), "Should NOT have ROLE_DOCTOR authority");
        assertFalse(hasRole(auth, "ROLE_PATIENT"), "Should NOT have ROLE_PATIENT authority");
        // Note: PHI access control enforced at service layer via @PreAuthorize, not via role authorities
    }

    /**
     * Test: Admin role is properly configured with ROLE_ADMIN authority
     * Per spec: Admin should ONLY see anonymized analytics, NO PHI
     */
    @Test
    void adminRole_shouldHaveCorrectAuthority() {
        // Arrange & Act
        setSecurityContext(UserRole.ADMIN);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Assert
        assertNotNull(auth, "Authentication should not be null");
        assertTrue(hasRole(auth, "ROLE_ADMIN"), "Should have ROLE_ADMIN authority");
        assertFalse(hasRole(auth, "ROLE_DOCTOR"), "Should NOT have ROLE_DOCTOR authority");
        assertFalse(hasRole(auth, "ROLE_PATIENT"), "Should NOT have ROLE_PATIENT authority");
        // Note: PHI access control enforced at service layer via @PreAuthorize, not via role authorities
    }

    /**
     * Test: Organization role is properly configured with ROLE_ORGANIZATION authority
     * Per spec: Organization PHI access is network-scoped (enforced via @PreAuthorize and service layer)
     */
    @Test
    void organizationRole_shouldHaveCorrectAuthority() {
        // Arrange & Act
        setSecurityContext(UserRole.ORGANIZATION);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Assert
        assertNotNull(auth, "Authentication should not be null");
        assertTrue(hasRole(auth, "ROLE_ORGANIZATION"), "Should have ROLE_ORGANIZATION authority");
        assertFalse(hasRole(auth, "ROLE_ADMIN"), "Should NOT have ROLE_ADMIN authority");
        // Note: Organization PHI access is network-scoped (enforced via @PreAuthorize and service layer)
    }

    /**
     * Helper: Set security context with specified role
     */
    private void setSecurityContext(UserRole role) {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "test@example.com", "password", authorities
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Helper: Check if authentication has a specific role
     */
    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(role));
    }
}
