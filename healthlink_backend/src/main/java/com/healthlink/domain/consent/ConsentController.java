package com.healthlink.domain.consent;

import com.healthlink.domain.consent.dto.AcceptConsentRequest;
import com.healthlink.domain.consent.dto.ConsentVersionResponse;
import com.healthlink.domain.consent.dto.UserConsentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.healthlink.security.model.CustomUserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/consent")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','STAFF','ORGANIZATION','ADMIN')")
    public List<ConsentVersionResponse> listActive() {
        return consentService.listActive();
    }

    @GetMapping("/latest/{lang}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','STAFF','ORGANIZATION','ADMIN')")
    public ConsentVersionResponse latest(@PathVariable String lang) {
        return consentService.latestByLanguage(lang);
    }

    @PostMapping("/accept")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','STAFF','ORGANIZATION','ADMIN')")
    public UserConsentResponse accept(Authentication auth, @Valid @RequestBody AcceptConsentRequest request) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails cud)) {
            throw new IllegalStateException("Authenticated user required");
        }
        return consentService.accept(cud.getId(), request);
    }
}