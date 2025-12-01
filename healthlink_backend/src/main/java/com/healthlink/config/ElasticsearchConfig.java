package com.healthlink.config;

import com.healthlink.infrastructure.logging.SafeLogger;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import com.healthlink.domain.search.document.DoctorDocument;

import jakarta.annotation.PostConstruct;

/**
 * Ensures required Elasticsearch indices exist with minimal, safe defaults.
 * Avoids dynamic mapping surprises and enforces predictable tokenization.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.data.elasticsearch.repositories", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ElasticsearchConfig {

    private static final SafeLogger log = SafeLogger.getLogger(ElasticsearchConfig.class);
    private final ElasticsearchOperations operations;

    public ElasticsearchConfig(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @PostConstruct
    public void ensureIndices() {
        try {
            IndexCoordinates coords = IndexCoordinates.of("doctors");
            IndexOperations io = operations.indexOps(coords);
            if (!io.exists()) {
                // Create index with simple settings (could be expanded later)
                // Minimal settings: 1 shard, 0 replicas for dev environment
                io.create();
                // Apply mapping from entity
                io.putMapping(operations.indexOps(DoctorDocument.class).createMapping(DoctorDocument.class));
                log.info("Elasticsearch index 'doctors' created");
            } else {
                log.info("Elasticsearch index 'doctors' exists");
            }
        } catch (Exception e) {
            log.warn("Elasticsearch index initialization failed: {}", e.getMessage());
        }
    }
}
