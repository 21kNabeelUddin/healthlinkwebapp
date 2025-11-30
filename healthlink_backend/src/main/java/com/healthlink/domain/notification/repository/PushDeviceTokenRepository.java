package com.healthlink.domain.notification.repository;

import com.healthlink.domain.notification.entity.PushDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushDeviceTokenRepository extends JpaRepository<PushDeviceToken, UUID> {
    List<PushDeviceToken> findByUserId(UUID userId);
    Optional<PushDeviceToken> findByToken(String token);
}
