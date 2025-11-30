package com.healthlink.security.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for RefreshTokenService
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private RefreshToken testToken;
    private UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 604800000L); // 7 days

        testToken = new RefreshToken();
        testToken.setId(UUID.randomUUID());
        testToken.setToken("test-refresh-token");
        testToken.setUserId(userId);
        testToken.setFamilyId(UUID.randomUUID().toString());
        testToken.setExpiresAt(OffsetDateTime.now().plusDays(7));
        testToken.setRevoked(false);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void store_shouldSaveTokenWithFamilyId() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken result = refreshTokenService.saveRefreshToken("new-token", userId, 604800000L);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("new-token");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getFamilyId()).isNotNull(); // Should initialize family ID
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void isRevoked_shouldReturnTrueForRevokedToken() {
        testToken.setRevoked(true);
        when(repository.findByToken("test-token")).thenReturn(Optional.of(testToken));

        boolean result = refreshTokenService.isRevoked("test-token");

        assertThat(result).isTrue();
    }

    @Test
    void isRevoked_shouldReturnTrueForNonExistentToken() {
        when(repository.findByToken("non-existent")).thenReturn(Optional.empty());

        boolean result = refreshTokenService.isRevoked("non-existent");

        assertThat(result).isTrue(); // Treat as revoked
    }

    @Test
    void revoke_shouldMarkAsRevokedAndBlacklist() {
        when(repository.findByToken(testToken.getToken())).thenReturn(Optional.of(testToken));
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        refreshTokenService.revoke(testToken.getToken());

        assertThat(testToken.isRevoked()).isTrue();
        assertThat(testToken.getRevokedAt()).isNotNull();
        verify(repository).save(testToken);
        verify(valueOperations).set(eq("refresh:blacklist:" + testToken.getToken()), eq("1"), any(Duration.class));
    }

    @Test
    void rotate_shouldCreateNewTokenAndRevokeOld() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken newToken = refreshTokenService.rotate(testToken, "new-token", 604800000L);

        assertThat(testToken.isRevoked()).isTrue();
        assertThat(newToken).isNotNull();
        assertThat(newToken.getToken()).isEqualTo("new-token");
        assertThat(newToken.getFamilyId()).isEqualTo(testToken.getFamilyId()); // Same family
        assertThat(newToken.getPreviousId()).isEqualTo(testToken.getId());
    }

    @Test
    void rotate_shouldBlacklistOldToken() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        refreshTokenService.rotate(testToken, "new-token", 604800000L);

        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(
                eq("refresh:blacklist:" + testToken.getToken()),
                eq("1"),
                durationCaptor.capture());

        assertThat(durationCaptor.getValue().toMillis()).isEqualTo(604800000L); // TTL should match refresh expiration
    }

    @Test
    void defensiveFamilyRevocation_shouldRevokeAllTokensWhenExceedingLimit() {
        UUID familyId = UUID.randomUUID();
        List<RefreshToken> familyTokens = Arrays.asList(
                createToken(familyId, false),
                createToken(familyId, false),
                createToken(familyId, false),
                createToken(familyId, false),
                createToken(familyId, false),
                createToken(familyId, false),
                createToken(familyId, false),
                createToken(familyId, false),
                createToken(familyId, false),
                createToken(familyId, false),
                createToken(familyId, false) // 11 tokens, exceeds max of 10
        );

        when(repository.findByFamilyIdOrderByRevokedAsc(familyId.toString())).thenReturn(familyTokens);
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        refreshTokenService.defensiveFamilyRevocation(familyId.toString(), 10);

        for (RefreshToken token : familyTokens) {
            assertThat(token.isRevoked()).isTrue();
        }
        verify(repository, times(11)).save(any(RefreshToken.class));
    }

    @Test
    void isBlacklisted_shouldReturnTrueForBlacklistedToken() {
        when(redisTemplate.hasKey("refresh:blacklist:test-token")).thenReturn(true);

        boolean result = refreshTokenService.isBlacklisted("test-token");

        assertThat(result).isTrue();
    }

    @Test
    void revokeAllForUser_shouldRevokeAllUserTokens() {
        List<RefreshToken> userTokens = Arrays.asList(
                createToken(UUID.randomUUID(), false),
                createToken(UUID.randomUUID(), false),
                createToken(UUID.randomUUID(), true) // Already revoked
        );

        when(repository.findByUserId(userId)).thenReturn(userTokens);
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        refreshTokenService.revokeAllForUser(userId);

        for (RefreshToken token : userTokens) {
            assertThat(token.isRevoked()).isTrue();
        }
        verify(repository, times(2)).save(any(RefreshToken.class));
        verify(valueOperations, times(3)).set(anyString(), eq("1"), any(Duration.class));
    }

    @Test
    void cleanupExpiredTokens_shouldDeleteExpiredTokens() {
        List<RefreshToken> expiredTokens = Arrays.asList(
                createExpiredToken(),
                createExpiredToken());

        when(repository.findByExpiresAtBefore(any(OffsetDateTime.class))).thenReturn(expiredTokens);

        int count = refreshTokenService.cleanupExpiredTokens();

        assertThat(count).isEqualTo(2);
        verify(repository).deleteAll(expiredTokens);
    }

    private RefreshToken createToken(UUID familyId, boolean revoked) {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setToken("token-" + token.getId());
        token.setUserId(userId);
        token.setFamilyId(familyId.toString());
        token.setRevoked(revoked);
        token.setExpiresAt(OffsetDateTime.now().plusDays(7));
        return token;
    }

    private RefreshToken createExpiredToken() {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setToken("expired-" + token.getId());
        token.setUserId(userId);
        token.setExpiresAt(OffsetDateTime.now().minusDays(1));
        return token;
    }
}
