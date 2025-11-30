package com.healthlink.domain.user.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for User entity business logic
 */
class UserEntityTest {

    private TestUser user;

    @BeforeEach
    void setUp() {
        user = new TestUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setIsActive(true);
        user.setIsEmailVerified(true);
        user.setApprovalStatus(com.healthlink.domain.user.enums.ApprovalStatus.APPROVED);
        user.setFailedLoginAttempts(0);
    }

    @Test
    void getFullName_shouldCombineFirstAndLastName() {
        String fullName = user.getFullName();

        assertThat(fullName).isEqualTo("John Doe");
    }

    @Test
    void getFullName_shouldReturnFullNameIfSet() {
        user.setFullName("Dr. John Doe");

        String fullName = user.getFullName();

        assertThat(fullName).isEqualTo("Dr. John Doe");
    }

    @Test
    void getFullName_shouldFallbackToEmailWhenNamesNull() {
        user.setFirstName(null);
        user.setLastName(null);
        user.setFullName(null);

        String fullName = user.getFullName();

        assertThat(fullName).isEqualTo("test@example.com");
    }

    @Test
    void canLogin_shouldReturnTrueForActiveApprovedVerifiedUser() {
        boolean canLogin = user.canLogin();

        assertThat(canLogin).isTrue();
    }

    @Test
    void canLogin_shouldReturnFalseForInactiveUser() {
        user.setIsActive(false);

        boolean canLogin = user.canLogin();

        assertThat(canLogin).isFalse();
    }

    @Test
    void canLogin_shouldReturnFalseForUnverifiedEmail() {
        user.setIsEmailVerified(false);

        boolean canLogin = user.canLogin();

        assertThat(canLogin).isFalse();
    }

    @Test
    void canLogin_shouldReturnFalseForUnapprovedUser() {
        user.setApprovalStatus(com.healthlink.domain.user.enums.ApprovalStatus.PENDING);

        boolean canLogin = user.canLogin();

        assertThat(canLogin).isFalse();
    }

    @Test
    void canLogin_shouldReturnFalseForLockedAccount() {
        user.setAccountLockedUntil(Instant.now().plusSeconds(1800));

        boolean canLogin = user.canLogin();

        assertThat(canLogin).isFalse();
    }

    @Test
    void markTokensRevoked_shouldSetTokensRevokedAt() {
        user.markTokensRevoked();

        assertThat(user.getTokensRevokedAt()).isNotNull();
        assertThat(user.getTokensRevokedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void softDelete_shouldSetDeletedFields() {
        UUID deletedBy = UUID.randomUUID();

        user.softDelete(deletedBy);

        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getDeletedBy()).isEqualTo(deletedBy);
        assertThat(user.getIsActive()).isFalse();
    }

    @Test
    void isDeleted_shouldReturnTrueWhenDeleted() {
        user.softDelete(UUID.randomUUID());

        assertThat(user.isDeleted()).isTrue();
    }

    @Test
    void isDeleted_shouldReturnFalseWhenNotDeleted() {
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    void incrementFailedLoginAttempts_shouldIncrementCounter() {
        user.incrementFailedLoginAttempts();

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void incrementFailedLoginAttempts_shouldLockAccountAfter5Attempts() {
        for (int i = 0; i < 5; i++) {
            user.incrementFailedLoginAttempts();
        }

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getAccountLockedUntil()).isNotNull();
        assertThat(user.getAccountLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void resetFailedLoginAttempts_shouldResetCounterAndUnlock() {
        user.setFailedLoginAttempts(5);
        user.setAccountLockedUntil(Instant.now().plusSeconds(1800));

        user.resetFailedLoginAttempts();

        assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(user.getAccountLockedUntil()).isNull();
    }

    /**
     * Test implementation of User for testing
     */
    private static class TestUser extends User {
        // Concrete implementation for testing abstract User class
    }
}
