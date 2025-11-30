package com.healthlink.domain.notification;

import com.healthlink.domain.notification.entity.Notification;
import com.healthlink.security.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for in-app notification retrieval and acknowledgement.
 * Spec requirement: In-app only notifications with configurable reminders.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','STAFF','ORGANIZATION','ADMIN')")
    public List<Notification> list(Authentication auth) {
        CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();
        return service.listForUser(cud.getId());
    }

    @PostMapping("/{id}/ack")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','STAFF','ORGANIZATION','ADMIN')")
    public Notification acknowledge(@PathVariable UUID id, Authentication auth) {
        CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();
        return service.acknowledge(id, cud.getId());
    }
}
