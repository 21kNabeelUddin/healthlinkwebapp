package com.healthlink.domain.record.service;

import com.healthlink.domain.record.dto.DrugInteractionRequest;
import com.healthlink.domain.record.dto.DrugInteractionResponse;
import com.healthlink.infrastructure.logging.SafeLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Drug interaction checking service using OpenFDA with parallel fetching and severity classification.
 * <p>
 * <strong>HIPAA Compliance:</strong>
 * <ul>
 *     <li>Uses {@link SafeLogger} to ensure no PHI is logged during interaction checks.</li>
 *     <li>Only drug names (not patient IDs) are processed here.</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class DrugInteractionService {

    private final OpenFdaDrugInteractionClient fdaClient;
    private final SafeLogger log = SafeLogger.get(DrugInteractionService.class);
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    // High-risk drugs requiring special attention
    private static final Set<String> HIGH_RISK_DRUGS = Set.of(
            "warfarin", "heparin", "digoxin", "lithium", "phenytoin", 
            "methotrexate", "insulin", "nitroglycerin", "sildenafil"
    );

    public DrugInteractionResponse checkInteractions(DrugInteractionRequest request) {
        List<String> drugs = Optional.ofNullable(request.getDrugNames())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .toList();
        
        if (drugs.isEmpty()) {
            return DrugInteractionResponse.builder()
                    .interactingPairs(List.of())
                    .warnings(List.of())
                    .severeInteractionDetected(false)
                    .build();
        }

        if (drugs.size() == 1) {
            // Single drug: fetch general warnings
            List<String> singleDrugWarnings = fdaClient.fetchInteractions(drugs.get(0));
            return DrugInteractionResponse.builder()
                    .interactingPairs(List.of())
                    .warnings(singleDrugWarnings)
                    .severeInteractionDetected(isHighRisk(drugs.get(0)))
                    .build();
        }

        log.event("drug_interaction_check")
           .with("drugCount", String.valueOf(drugs.size()))
           .with("drugs", String.join(",", drugs))
           .log();

        // Parallel fetch interactions for all drugs
        List<CompletableFuture<DrugInteractionData>> futures = drugs.stream()
                .map(drug -> CompletableFuture.supplyAsync(() -> new DrugInteractionData(drug, fdaClient.fetchInteractions(drug)), executorService))
                .toList();

        List<DrugInteractionData> allData = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        // Cross-reference pairwise interactions
        List<String> interactingPairs = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> processedPairs = new HashSet<>();

        for (int i = 0; i < allData.size(); i++) {
            DrugInteractionData dataA = allData.get(i);
            
            for (int j = i + 1; j < allData.size(); j++) {
                DrugInteractionData dataB = allData.get(j);
                String pairKey = createPairKey(dataA.drugName, dataB.drugName);
                
                if (processedPairs.contains(pairKey)) continue;
                processedPairs.add(pairKey);

                // Check if drugB mentioned in drugA's interactions or vice versa
                boolean interactionFound = dataA.interactions.stream()
                        .anyMatch(text -> text.toLowerCase().contains(dataB.drugName.toLowerCase()))
                        || dataB.interactions.stream()
                        .anyMatch(text -> text.toLowerCase().contains(dataA.drugName.toLowerCase()));

                if (interactionFound) {
                    String pairLabel = dataA.drugName + " + " + dataB.drugName;
                    interactingPairs.add(pairLabel);
                    
                    // Extract relevant warnings mentioning both drugs
                    dataA.interactions.stream()
                            .filter(text -> text.toLowerCase().contains(dataB.drugName.toLowerCase()))
                            .forEach(text -> warnings.add("[" + pairLabel + "] " + text));
                    
                    dataB.interactions.stream()
                            .filter(text -> text.toLowerCase().contains(dataA.drugName.toLowerCase()))
                            .forEach(text -> warnings.add("[" + pairLabel + "] " + text));
                }
            }
        }

        // Add general warnings for high-risk drugs even without direct interactions
        drugs.stream()
                .filter(this::isHighRisk)
                .forEach(drug -> warnings.add("⚠️ " + drug + " is a high-risk medication. Monitor closely."));

        boolean severe = drugs.stream().anyMatch(this::isHighRisk) || !interactingPairs.isEmpty();

        log.event("drug_interaction_check_complete")
           .with("interactionCount", String.valueOf(interactingPairs.size()))
           .with("severe", String.valueOf(severe))
           .log();

        return DrugInteractionResponse.builder()
                .interactingPairs(interactingPairs)
                .warnings(warnings.isEmpty() ? List.of("No significant interactions detected") : warnings)
                .severeInteractionDetected(severe)
                .build();
    }

    private String createPairKey(String drugA, String drugB) {
        List<String> sorted = List.of(drugA.toLowerCase(), drugB.toLowerCase()).stream().sorted().toList();
        return sorted.get(0) + "|" + sorted.get(1);
    }

    private boolean isHighRisk(String drugName) {
        return HIGH_RISK_DRUGS.stream().anyMatch(risk -> drugName.toLowerCase().contains(risk));
    }

    private record DrugInteractionData(String drugName, List<String> interactions) {}
}
