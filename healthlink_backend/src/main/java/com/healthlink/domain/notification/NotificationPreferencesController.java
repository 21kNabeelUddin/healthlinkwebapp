package com.healthlink.domain.notification;

import com.healthlink.domain.notification.dto.NotificationPreferenceResponse;
import com.healthlink.domain.notification.dto.UpdateNotificationPreferenceRequest;
import com.healthlink.security.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferencesController {

    private final NotificationPreferenceService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','STAFF','ORGANIZATION','ADMIN')")
    public NotificationPreferenceResponse get(Authentication auth) {
        CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();
        return service.get(cud.getId());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','STAFF','ORGANIZATION','ADMIN')")
    public NotificationPreferenceResponse update(Authentication auth, @RequestBody UpdateNotificationPreferenceRequest request) {
        CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();
        return service.update(cud.getId(), request);
    }
}