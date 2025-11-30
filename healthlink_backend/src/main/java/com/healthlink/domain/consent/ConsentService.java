package com.healthlink.domain.consent;

import com.healthlink.domain.consent.dto.AcceptConsentRequest;
import com.healthlink.domain.consent.dto.ConsentVersionResponse;
import com.healthlink.domain.consent.dto.UserConsentResponse;
import com.healthlink.domain.consent.entity.ConsentVersion;
import com.healthlink.domain.consent.entity.UserConsent;
import com.healthlink.domain.consent.repository.ConsentVersionRepository;
import com.healthlink.domain.consent.repository.UserConsentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final ConsentVersionRepository consentVersionRepository;
    private final UserConsentRepository userConsentRepository;

    @Transactional(readOnly = true)
    public List<ConsentVersionResponse> listActive() {
        return consentVersionRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConsentVersionResponse latestByLanguage(String lang) {
        ConsentVersion cv = consentVersionRepository.findFirstByLanguageAndActiveTrueOrderByCreatedAtDesc(lang)
                .orElseThrow(() -> new EntityNotFoundException("Active consent version not found for language " + lang));
        return toResponse(cv);
    }

    @Transactional
    public UserConsentResponse accept(UUID userId, AcceptConsentRequest request) {
        ConsentVersion version = consentVersionRepository.findByConsentVersionAndLanguage(request.getVersion(), request.getLanguage())
                .orElseThrow(() -> new EntityNotFoundException("Consent version not found"));
        if (!version.isActive()) {
            throw new EntityNotFoundException("Consent version not active");
        }
        if (userConsentRepository.existsByUserIdAndConsentVersion(userId, version.getConsentVersion())) {
            UserConsent existing = userConsentRepository.findFirstByUserIdOrderByAcceptedAtDesc(userId).orElseThrow();
            return UserConsentResponse.builder()
                    .userId(existing.getUserId())
                    .consentVersion(existing.getConsentVersion())
                    .language(version.getLanguage())
                    .acceptedAt(existing.getAcceptedAt())
                    .build();
        }
        UserConsent uc = new UserConsent();
        uc.setUserId(userId);
        uc.setConsentVersion(version.getConsentVersion());
        userConsentRepository.save(uc);
        return UserConsentResponse.builder()
                .userId(userId)
                .consentVersion(version.getConsentVersion())
                .language(version.getLanguage())
                .acceptedAt(uc.getAcceptedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean hasAcceptedLatest(UUID userId, String lang) {
        ConsentVersion latest = consentVersionRepository.findFirstByLanguageAndActiveTrueOrderByCreatedAtDesc(lang).orElse(null);
        if (latest == null) return false;
        return userConsentRepository.existsByUserIdAndConsentVersion(userId, latest.getConsentVersion());
    }

    private ConsentVersionResponse toResponse(ConsentVersion cv) {
        return ConsentVersionResponse.builder()
                .version(cv.getConsentVersion())
                .language(cv.getLanguage())
                .content(cv.getContent())
                .active(cv.isActive())
                .build();
    }
}