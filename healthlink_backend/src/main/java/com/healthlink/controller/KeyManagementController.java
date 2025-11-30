package com.healthlink.controller;

import com.healthlink.security.encryption.PhiEncryptionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * KeyManagementController
 * ADMIN-only endpoints for inspecting and rotating PHI encryption keys.
 * NOTE: In production, key material must come from a secure vault/HSM; this endpoint
 * merely demonstrates rotation flow and should be protected and audited.
 */
@RestController
@RequestMapping("/api/v1/admin/keys")
@RequiredArgsConstructor
public class KeyManagementController {
    private final PhiEncryptionService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public KeyStatusResponse status() {
        KeyStatusResponse resp = new KeyStatusResponse();
        resp.setActiveAlias(service.getActiveAlias());
        resp.setAliases(service.getAllKeys().keySet());
        return resp;
    }

    @PostMapping("/rotate")
    @PreAuthorize("hasRole('ADMIN')")
    public RotationResponse rotate(@RequestBody RotateRequest request) {
        String newAlias = service.rotate(request.getAlias(), request.getKeyMaterial());
        RotationResponse resp = new RotationResponse();
        resp.setNewActiveAlias(newAlias);
        resp.setAllAliases(service.getAllKeys().keySet());
        return resp;
    }

    @Data
    public static class RotateRequest { private String alias; private String keyMaterial; }
    @Data
    public static class RotationResponse { private String newActiveAlias; private java.util.Set<String> allAliases; }
    @Data
    public static class KeyStatusResponse { private String activeAlias; private java.util.Set<String> aliases; }
}
