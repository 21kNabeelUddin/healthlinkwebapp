package com.healthlink.domain.user.controller;

import com.healthlink.domain.user.dto.CreateAdminRequest;
import com.healthlink.domain.user.dto.AdminUserResponse;
import com.healthlink.domain.user.service.PlatformOwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Platform Owner controller for managing admin accounts and final-level escalations.
 * Per spec: Platform Owner manages Admin accounts, has system-wide analytics, final dispute escalation.
 */
@RestController
@RequestMapping("/api/v1/platform-owner")
@Tag(name = "Platform Owner", description = "Platform Owner endpoints for admin management and system oversight")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_OWNER')")
public class PlatformOwnerController {

    private final PlatformOwnerService platformOwnerService;

    @PostMapping("/admins")
    @Operation(summary = "Create a new admin account", description = "Platform Owner can create admin accounts with username/password authentication")
    public ResponseEntity<AdminUserResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        AdminUserResponse admin = platformOwnerService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(admin);
    }

    @GetMapping("/admins")
    @Operation(summary = "List all admin accounts")
    public ResponseEntity<List<AdminUserResponse>> listAdmins() {
        return ResponseEntity.ok(platformOwnerService.listAdmins());
    }

    @PutMapping("/admins/{adminId}/deactivate")
    @Operation(summary = "Deactivate an admin account", description = "Prevents admin from logging in")
    public ResponseEntity<Void> deactivateAdmin(@PathVariable UUID adminId) {
        platformOwnerService.deactivateAdmin(adminId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admins/{adminId}/reactivate")
    @Operation(summary = "Reactivate a deactivated admin account")
    public ResponseEntity<Void> reactivateAdmin(@PathVariable UUID adminId) {
        platformOwnerService.reactivateAdmin(adminId);
        return ResponseEntity.noContent().build();
    }
}
