package com.healthlink.domain.webhook;

import com.healthlink.domain.webhook.dto.WebhookSubscriptionResponse;
import com.healthlink.domain.webhook.entity.WebhookSubscription;
import com.healthlink.domain.webhook.repository.WebhookSubscriptionRepository;
import com.healthlink.security.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks/subscriptions")
@RequiredArgsConstructor
public class WebhookSubscriptionController {
    private final WebhookSubscriptionRepository repository;

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ORGANIZATION','ADMIN')")
    public List<WebhookSubscriptionResponse> list(Authentication auth) {
        CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();
        return repository.findByOwnerUserId(cud.getId())
            .stream()
            .map(this::toDto)
            .toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ORGANIZATION','ADMIN')")
    public WebhookSubscriptionResponse create(Authentication auth, @RequestBody WebhookSubscription req) {
        CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();
        req.setId(null); // ensure new
        req.setOwnerUserId(cud.getId());
        return toDto(repository.save(req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ORGANIZATION','ADMIN')")
    public void delete(Authentication auth, @PathVariable UUID id) {
        CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();
        repository.findById(id).ifPresent(sub -> {
            if (sub.getOwnerUserId().equals(cud.getId())) {
                repository.delete(sub);
            }
        });
    }
    
    private WebhookSubscriptionResponse toDto(WebhookSubscription entity) {
        return WebhookSubscriptionResponse.builder()
            .id(entity.getId())
            .eventType(entity.getEventType())
            .targetUrl(entity.getTargetUrl())
            .active(entity.isActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}