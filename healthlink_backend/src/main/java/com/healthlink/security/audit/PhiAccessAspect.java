package com.healthlink.security.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.healthlink.security.model.CustomUserDetails;
import org.springframework.stereotype.Component;
import com.healthlink.domain.security.entity.AuditEvent;
import com.healthlink.domain.security.repository.AuditEventRepository;
import java.util.UUID;

/**
 * Aspect to record PHI access events. Methods annotated with @PhiAccess will produce audit logs.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PhiAccessAspect {

    private final AuditEventRepository auditEventRepository;

    @AfterReturning("@annotation(phiAccess)")
    public void afterPhiAccess(JoinPoint jp, PhiAccess phiAccess) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = null;
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            userId = cud.getId();
        }
        var event = new AuditEvent();
        event.setUserId(userId);
        event.setOperation(phiAccess.operation());
        event.setTargetRef(jp.getSignature().toShortString());
        event.setDetails("argsCount=" + jp.getArgs().length);
        auditEventRepository.save(event);
        log.info("PHI_ACCESS persisted operation={} target={}", phiAccess.operation(), jp.getSignature().toShortString());
    }
}