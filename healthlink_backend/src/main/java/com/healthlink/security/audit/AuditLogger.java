package com.healthlink.security.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Audit logger for system-level events (non-PHI).
 * Logs admin account management, approval decisions, dispute escalations, etc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogger {

    /**
     * Log admin account creation by Platform Owner
     */
    public void logAdminCreation(UUID adminId, String username) {
        log.info("AUDIT: Admin account created - ID: {}, Username: {}", adminId, username);
    }

    /**
     * Log admin account deactivation by Platform Owner
     */
    public void logAdminDeactivation(UUID adminId) {
        log.info("AUDIT: Admin account deactivated - ID: {}", adminId);
    }

    /**
     * Log admin account reactivation by Platform Owner
     */
    public void logAdminReactivation(UUID adminId) {
        log.info("AUDIT: Admin account reactivated - ID: {}", adminId);
    }

    /**
     * Log approval decision (doctor/organization)
     */
    public void logApprovalDecision(UUID userId, String decision, String reason) {
        log.info("AUDIT: Approval decision - User ID: {}, Decision: {}, Reason: {}", userId, decision, reason);
    }

    /**
     * Log payment dispute escalation
     */
    public void logDisputeEscalation(UUID disputeId, String fromStage, String toStage) {
        log.info("AUDIT: Payment dispute escalated - ID: {}, From: {}, To: {}", disputeId, fromStage, toStage);
    }

    /**
     * Log payment dispute resolution
     */
    public void logDisputeResolution(UUID disputeId, String resolution) {
        log.info("AUDIT: Payment dispute resolved - ID: {}, Resolution: {}", disputeId, resolution);
    }

    /**
     * Log PHI access event
     */
    public void logPhiAccess(String actor, String action, String resource, String reason) {
        // In a real system, this would go to a secure, immutable audit log.
        // For now, we log to the application log with a specific prefix, ensuring PHI
        // is sanitized if present in metadata.
        // The 'resource' and 'reason' should be sanitized by the caller or here.
        // We assume the caller uses PhiLoggingSanitizer.
        log.info("AUDIT: PHI Access - Actor: {}, Action: {}, Resource: {}, Reason: {}", actor, action, resource,
                reason);
    }
}
