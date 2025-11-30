package com.healthlink.controller;

import com.healthlink.dto.ResponseEnvelope;
import com.healthlink.service.notification.PushNotificationService;
import com.healthlink.domain.user.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing push notification device tokens.
 * Allows mobile clients to register FCM device tokens for receiving push notifications.
 */
@RestController
@RequestMapping("/api/v1/notifications/device")
@RequiredArgsConstructor
public class PushDeviceTokenController {

    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEnvelope<String> register(@RequestBody RegisterTokenRequest request, Authentication auth) {
        var user = userRepository.findByEmailAndDeletedAtIsNull(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        pushNotificationService.registerToken(user.getId(), request.getToken(), request.getPlatform());
        return ResponseEnvelope.<String>builder()
                .data("REGISTERED")
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId("push-register")
                .build();
    }

    @Data
    public static class RegisterTokenRequest {
        private String token;
        private String platform; // ANDROID / IOS / WEB
    }
}
