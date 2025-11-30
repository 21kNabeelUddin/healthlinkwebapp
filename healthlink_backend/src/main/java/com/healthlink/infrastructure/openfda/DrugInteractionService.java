package com.healthlink.infrastructure.openfda;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.CacheEvict;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Legacy implementation.
 * @deprecated Use {@link com.healthlink.domain.record.service.DrugInteractionService} instead.
 */
@Deprecated
public class DrugInteractionService {

    private final OpenFdaClient client;
    private final Set<String> highRiskCache = new HashSet<>();

    public DrugInteractionService(OpenFdaClient client) {
        this.client = client;
    }

    public InteractionResult evaluate(String drugA, String drugB) {
        String key = normalize(drugA) + ":" + normalize(drugB);
        if (highRiskCache.contains(key)) {
            return InteractionResult.highRisk("Cached high-risk interaction");
        }
        Optional<JsonNode> json = client.checkInteraction(normalize(drugA), normalize(drugB));
        if (json.isEmpty()) {
            return InteractionResult.none();
        }
        JsonNode root = json.get();
        if (root.path("results").isArray() && root.path("results").size() > 0) {
            // Simplified risk parsing
            String description = root.path("results").get(0).path("description").asText("Potential interaction");
            highRiskCache.add(key);
            return InteractionResult.highRisk(description);
        }
        return InteractionResult.none();
    }

    @CacheEvict(value = "drugInteractions", allEntries = true)
    public void clearCaches() {
        highRiskCache.clear();
    }

    private String normalize(String drug) {
        return drug.trim().toLowerCase();
    }

    public record InteractionResult(boolean interaction, String severity, String message) {
        public static InteractionResult highRisk(String msg) { return new InteractionResult(true, "HIGH", msg); }
        public static InteractionResult none() { return new InteractionResult(false, "NONE", "No known interaction"); }
    }
}
