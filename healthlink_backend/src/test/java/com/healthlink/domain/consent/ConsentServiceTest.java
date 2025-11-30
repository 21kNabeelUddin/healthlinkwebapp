package com.healthlink.domain.consent;

import com.healthlink.domain.consent.dto.AcceptConsentRequest;
import com.healthlink.domain.consent.repository.ConsentVersionRepository;
import com.healthlink.domain.consent.repository.UserConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ConsentServiceTest {

    private ConsentVersionRepository consentVersionRepository;
    private UserConsentRepository userConsentRepository;
    private ConsentService consentService;

    @BeforeEach
    void setup() {
        consentVersionRepository = Mockito.mock(ConsentVersionRepository.class);
        userConsentRepository = Mockito.mock(UserConsentRepository.class);
        consentService = new ConsentService(consentVersionRepository, userConsentRepository);
    }

    @Test
    void acceptStoresConsentWhenNotExisting() {
        UUID userId = UUID.randomUUID();
        var version = new com.healthlink.domain.consent.entity.ConsentVersion();
        version.setConsentVersion("v1.0");
        version.setLanguage("en");
        version.setContent("Test consent");
        version.setActive(true);
        when(consentVersionRepository.findByConsentVersionAndLanguage("v1.0", "en")).thenReturn(Optional.of(version));
        when(userConsentRepository.existsByUserIdAndConsentVersion(userId, "v1.0")).thenReturn(false);
        var response = consentService.accept(userId, buildRequest());
        assertEquals("v1.0", response.getConsentVersion());
        verify(userConsentRepository, times(1)).save(any());
    }

    @Test
    void acceptDoesNotDuplicateWhenAlreadyExists() {
        UUID userId = UUID.randomUUID();
        var version = new com.healthlink.domain.consent.entity.ConsentVersion();
        version.setConsentVersion("v1.0");
        version.setLanguage("en");
        version.setContent("Test consent");
        version.setActive(true);
        when(consentVersionRepository.findByConsentVersionAndLanguage("v1.0", "en")).thenReturn(Optional.of(version));
        when(userConsentRepository.existsByUserIdAndConsentVersion(userId, "v1.0")).thenReturn(true);
        var existing = new com.healthlink.domain.consent.entity.UserConsent();
        existing.setUserId(userId);
        existing.setConsentVersion("v1.0");
        when(userConsentRepository.findFirstByUserIdOrderByAcceptedAtDesc(userId)).thenReturn(Optional.of(existing));
        var response = consentService.accept(userId, buildRequest());
        assertEquals("v1.0", response.getConsentVersion());
        verify(userConsentRepository, times(0)).save(any());
    }

    private AcceptConsentRequest buildRequest() {
        var req = new AcceptConsentRequest();
        req.setVersion("v1.0");
        req.setLanguage("en");
        return req;
    }
}
