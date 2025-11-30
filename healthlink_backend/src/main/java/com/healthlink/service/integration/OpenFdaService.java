package com.healthlink.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenFDA API Integration Service
 * Provides drug interaction checking and drug information retrieval
 * API Docs: https://open.fda.gov/apis/drug/
 */
@Service
@Slf4j
public class OpenFdaService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${healthlink.integration.openfda.api-url:https://api.fda.gov}")
    private String openFdaApiUrl;

    @Value("${healthlink.integration.openfda.api-key:#{null}}")
    private String apiKey; // Optional - higher rate limits if provided

    private static final String DRUG_LABEL_ENDPOINT = "/drug/label.json";

    public OpenFdaService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Check for drug interactions between multiple drugs
     * Cached for 24 hours to reduce API calls
     */
    @Cacheable(value = "drugInteractions", key = "#drugNames.hashCode()")
    public DrugInteractionResult checkDrugInteractions(List<String> drugNames) {
        log.info("Checking drug interactions for: {}", drugNames);

        DrugInteractionResult result = new DrugInteractionResult();
        result.setDrugs(drugNames);
        result.setInteractions(new ArrayList<>());

        for (String drugName : drugNames) {
            try {
                DrugInfo drugInfo = getDrugInfo(drugName);
                if (drugInfo != null && drugInfo.getInteractions() != null) {
                    result.getInteractions().addAll(drugInfo.getInteractions());
                }
            } catch (Exception e) {
                log.error("Failed to fetch drug info for: {}", drugName, e);
            }
        }

        result.setHasInteractions(!result.getInteractions().isEmpty());
        return result;
    }

    /**
     * Get comprehensive drug information from OpenFDA
     */
    @Cacheable(value = "drugInfo", key = "#drugName")
    public DrugInfo getDrugInfo(String drugName) {
        try {
            String url = buildDrugLabelUrl(drugName);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseDrugLabelResponse(response.getBody());
            } else {
                log.warn("Failed to fetch drug info for: {} - Status: {}",
                        drugName, response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("Error fetching drug info for: {}", drugName, e);
            return null;
        }
    }

    /**
     * Build OpenFDA API URL for drug label search
     */
    private String buildDrugLabelUrl(String drugName) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(openFdaApiUrl + DRUG_LABEL_ENDPOINT)
                .queryParam("search", "openfda.brand_name:\"" + drugName + "\"")
                .queryParam("limit", "1");

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.queryParam("api_key", apiKey);
        }

        return builder.toUriString();
    }

    /**
     * Parse OpenFDA drug label response
     */
    private DrugInfo parseDrugLabelResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode results = root.get("results");

            if (results == null || results.isEmpty()) {
                return null;
            }

            JsonNode drug = results.get(0);
            DrugInfo info = new DrugInfo();

            // Extract drug name
            JsonNode brandName = drug.get("openfda").get("brand_name");
            if (brandName != null && brandName.isArray() && brandName.size() > 0) {
                info.setName(brandName.get(0).asText());
            }

            // Extract generic name
            JsonNode genericName = drug.get("openfda").get("generic_name");
            if (genericName != null && genericName.isArray() && genericName.size() > 0) {
                info.setGenericName(genericName.get(0).asText());
            }

            // Extract warnings/interactions
            JsonNode warnings = drug.get("warnings");
            if (warnings != null && warnings.isArray()) {
                List<String> interactionList = new ArrayList<>();
                for (JsonNode warning : warnings) {
                    String warningText = warning.asText().toLowerCase();
                    if (warningText.contains("interaction") || warningText.contains("avoid")) {
                        interactionList.add(warning.asText());
                    }
                }
                info.setInteractions(interactionList);
            }

            // Extract purpose/indications
            JsonNode indications = drug.get("indications_and_usage");
            if (indications != null && indications.isArray() && indications.size() > 0) {
                info.setIndications(indications.get(0).asText());
            }

            return info;

        } catch (Exception e) {
            log.error("Failed to parse OpenFDA response", e);
            return null;
        }
    }

    /**
     * Drug Information DTO
     */
    @lombok.Data
    public static class DrugInfo {
        private String name;
        private String genericName;
        private String indications;
        private List<String> interactions;
    }

    /**
     * Drug Interaction Result DTO
     */
    @lombok.Data
    public static class DrugInteractionResult {
        private List<String> drugs;
        private boolean hasInteractions;
        private List<String> interactions;
    }
}
