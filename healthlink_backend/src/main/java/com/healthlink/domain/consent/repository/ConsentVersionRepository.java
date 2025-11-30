package com.healthlink.domain.consent.repository;

import com.healthlink.domain.consent.entity.ConsentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface ConsentVersionRepository extends JpaRepository<ConsentVersion, UUID> {
    Optional<ConsentVersion> findFirstByActiveTrueOrderByCreatedAtDesc();
    List<ConsentVersion> findByActiveTrueOrderByCreatedAtDesc();
    Optional<ConsentVersion> findByConsentVersion(String consentVersion);
    Optional<ConsentVersion> findFirstByLanguageAndActiveTrueOrderByCreatedAtDesc(String language);
    Optional<ConsentVersion> findByConsentVersionAndLanguage(String consentVersion, String language);
}