package com.healthlink.security.audit;

import com.healthlink.infrastructure.security.AuditEnrichmentFilter;
import com.healthlink.logging.PhiLoggingSanitizer;
import com.healthlink.security.annotation.PhiAccess;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * AOP aspect for PHI access logging.
 * Captures all access to PHI-containing endpoints and stores audit trail.
 * Uses MDC values populated by AuditEnrichmentFilter for enriched context.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class PhiAccessLoggingAspect {

    private final PhiAccessLogRepository repository;

    @AfterReturning("@annotation(phiAccess)")
    public void logAccess(JoinPoint joinPoint, PhiAccess phiAccess) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return; // Unauthenticated access should be blocked earlier by security
        }

        String username = auth.getName();
        String role = resolvePrimaryRole(auth.getAuthorities());
        String entityId = resolveEntityId(joinPoint, phiAccess.idParam());

        // Extract enriched audit data from MDC (populated by AuditEnrichmentFilter)
        String requestId = MDC.get(AuditEnrichmentFilter.MDC_REQUEST_ID);
        String traceId = MDC.get(AuditEnrichmentFilter.MDC_TRACE_ID);
        String clientIp = MDC.get(AuditEnrichmentFilter.MDC_CLIENT_IP);
        String userAgent = MDC.get(AuditEnrichmentFilter.MDC_USER_AGENT);

        // Fallback values if MDC not populated (shouldn't happen in normal flow)
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = "unknown";
        }
        if (userAgent == null || userAgent.isEmpty()) {
            userAgent = "unknown";
        }

        // Sanitize reason (may occasionally embed contextual identifiers)
        String sanitizedReason = PhiLoggingSanitizer.sanitizeReason(phiAccess.reason());

        PhiAccessLog log = PhiAccessLog.builder()
            .username(username)
            .role(role)
            .entityType(phiAccess.entityType().getSimpleName())
            .entityId(PhiLoggingSanitizer.sanitizeIdentifier(entityId))
            .reason(sanitizedReason)
            .accessedAt(Instant.now())
            .ipAddress(clientIp)
            .userAgent(userAgent)
            .traceId(traceId)
            .requestId(requestId)
            .build();
        
        repository.save(log);
        // Also emit generic PHI access analytics event (non-PHI metadata only)
        if (analyticsEventService != null) {
            analyticsEventService.record(com.healthlink.analytics.AnalyticsEventType.PHI_ACCESS, username, entityId, phiAccess.reason());
        }
    }

    private String resolvePrimaryRole(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().findFirst().map(GrantedAuthority::getAuthority).orElse("UNKNOWN");
    }

    private String resolveEntityId(JoinPoint joinPoint, String idParamName) {
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        String[] paramNames = codeSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(idParamName)) {
                return String.valueOf(args[i]);
            }
        }
        return "unknown";
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.healthlink.analytics.AnalyticsEventService analyticsEventService;
}
