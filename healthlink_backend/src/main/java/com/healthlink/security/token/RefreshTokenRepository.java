package com.healthlink.security.token;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserId(UUID userId);

    List<RefreshToken> findByFamilyIdOrderByRevokedAsc(String familyId);

    List<RefreshToken> findByExpiresAtBefore(java.time.OffsetDateTime dateTime);
}
