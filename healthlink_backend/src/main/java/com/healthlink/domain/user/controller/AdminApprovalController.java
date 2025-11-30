package com.healthlink.domain.user.controller;

import com.healthlink.domain.user.dto.ApprovalDecisionRequest;
import com.healthlink.domain.user.dto.PendingApprovalDto;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.service.AdminApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin controller for managing doctor and organization approval workflow.
 * Per spec: Admin approves/rejects via portal, sends email notifications.
 */
@RestController
@RequestMapping("/api/v1/admin/approvals")
@Tag(name = "Admin Approvals", description = "Admin endpoints for reviewing and approving/rejecting registrations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApprovalController {

    private final AdminApprovalService adminApprovalService;

    @GetMapping("/pending")
    @Operation(summary = "Get all pending approval requests (doctors and organizations)")
    @ApiResponse(responseCode = "200", description = "List of pending approvals")
    public ResponseEntity<List<PendingApprovalDto>> getPendingApprovals() {
        return ResponseEntity.ok(adminApprovalService.getPendingApprovals());
    }

    @GetMapping("/pending/doctors")
    @Operation(summary = "Get pending doctor approval requests")
    @ApiResponse(responseCode = "200", description = "List of pending doctors")
    public ResponseEntity<List<PendingApprovalDto>> getPendingDoctors() {
        return ResponseEntity.ok(adminApprovalService.getPendingApprovalsByRole(UserRole.DOCTOR));
    }

    @GetMapping("/pending/organizations")
    @Operation(summary = "Get pending organization approval requests")
    @ApiResponse(responseCode = "200", description = "List of pending organizations")
    public ResponseEntity<List<PendingApprovalDto>> getPendingOrganizations() {
        return ResponseEntity.ok(adminApprovalService.getPendingApprovalsByRole(UserRole.ORGANIZATION));
    }

    @PostMapping("/{userId}/decision")
    @Operation(summary = "Approve or reject a user account", description = "Admin reviews PMDC verification (for doctors) or organization number (for orgs) and approves/rejects. Email notification sent to user.")
    @ApiResponse(responseCode = "204", description = "Decision processed")
    public ResponseEntity<Void> processApproval(
            @PathVariable UUID userId,
            @Valid @RequestBody ApprovalDecisionRequest request) {
        adminApprovalService.processApprovalDecision(userId, request);
        return ResponseEntity.noContent().build();
    }
}
