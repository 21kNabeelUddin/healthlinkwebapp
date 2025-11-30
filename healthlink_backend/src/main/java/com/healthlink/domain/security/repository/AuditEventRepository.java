package com.healthlink.domain.security.repository;

import com.healthlink.domain.security.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByUserIdOrderByOccurredAtDesc(UUID userId);
    List<AuditEvent> findByOperationOrderByOccurredAtDesc(String operation);
}
