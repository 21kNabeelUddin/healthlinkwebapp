package com.healthlink.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventService {
    private final AnalyticsEventRepository repository;

    @Transactional
    public void record(AnalyticsEventType type, String actor, String subjectId, String meta) {
        try {
            AnalyticsEvent ev = AnalyticsEvent.builder()
                    .type(type.name())
                    .actor(actor == null ? "system" : actor)
                    .subjectId(subjectId)
                    .meta(meta != null && meta.length() > 1000 ? meta.substring(0,1000) : meta)
                    .occurredAt(OffsetDateTime.now())
                    .build();
            repository.save(ev);
        } catch (Exception e) {
            log.warn("Failed to persist analytics event {}: {}", type, e.getMessage());
        }
    }
}
