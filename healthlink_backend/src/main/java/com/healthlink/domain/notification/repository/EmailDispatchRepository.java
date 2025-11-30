package com.healthlink.domain.notification.repository;

import com.healthlink.domain.notification.entity.EmailDispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailDispatchRepository extends JpaRepository<EmailDispatch, UUID> {
    List<EmailDispatch> findByUserIdOrderByAttemptedAtDesc(UUID userId);
    List<EmailDispatch> findByEmailTypeAndUserIdOrderByAttemptedAtDesc(String emailType, UUID userId);
}
