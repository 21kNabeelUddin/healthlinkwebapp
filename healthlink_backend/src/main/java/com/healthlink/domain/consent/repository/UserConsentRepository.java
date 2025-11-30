package com.healthlink.domain.consent.repository;

import com.healthlink.domain.consent.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {
    Optional<UserConsent> findFirstByUserIdOrderByAcceptedAtDesc(UUID userId);
    boolean existsByUserIdAndConsentVersion(UUID userId, String consentVersion);
}