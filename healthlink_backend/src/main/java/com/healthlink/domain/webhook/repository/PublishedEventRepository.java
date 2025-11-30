package com.healthlink.domain.webhook.repository;

import com.healthlink.domain.webhook.entity.PublishedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PublishedEventRepository extends JpaRepository<PublishedEvent, UUID> {
}