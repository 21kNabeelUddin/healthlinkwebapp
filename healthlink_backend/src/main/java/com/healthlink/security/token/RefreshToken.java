package com.healthlink.security.token;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_user", columnList = "user_id"),
    @Index(name = "idx_refresh_revoked", columnList = "revoked"),
    @Index(name = "idx_refresh_family", columnList = "family_id")
})
@Getter @Setter
public class RefreshToken {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "token", nullable = false, length = 512, unique = true)
    private String token;
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;
    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId; // group of rotated tokens
    @Column(name = "previous_id")
    private UUID previousId; // immediate predecessor
    @Column(name = "replaced_by_id")
    private UUID replacedById; // successor when rotated
    @PrePersist
    void prePersist() {
        if (expiresAt == null) {
            expiresAt = OffsetDateTime.now().plusDays(7);
        }
        if (familyId == null) {
            familyId = UUID.randomUUID().toString();
        }
    }
}
