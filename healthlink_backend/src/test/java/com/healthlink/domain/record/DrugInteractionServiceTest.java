package com.healthlink.domain.record;

import com.healthlink.domain.record.dto.DrugInteractionRequest;
import com.healthlink.domain.record.dto.DrugInteractionResponse;
import com.healthlink.domain.record.service.DrugInteractionService;
import com.healthlink.domain.record.service.OpenFdaDrugInteractionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrugInteractionServiceTest {

    @Mock
    private OpenFdaDrugInteractionClient fdaClient;

    private DrugInteractionService service;

    @BeforeEach
    void setUp() {
        service = new DrugInteractionService(fdaClient);
    }

    @Test
    void shouldHandleEmptyRequest() {
        DrugInteractionRequest request = new DrugInteractionRequest();
        request.setDrugNames(Collections.emptyList());

        DrugInteractionResponse response = service.checkInteractions(request);

        assertNotNull(response);
        assertFalse(response.isSevereInteractionDetected());
        assertTrue(response.getInteractingPairs().isEmpty());
        assertTrue(response.getWarnings().isEmpty());
    }

    @Test
    void shouldHandleNullDrugList() {
        DrugInteractionRequest request = new DrugInteractionRequest();
        request.setDrugNames(null);

        DrugInteractionResponse response = service.checkInteractions(request);

        assertNotNull(response);
        assertFalse(response.isSevereInteractionDetected());
        assertTrue(response.getInteractingPairs().isEmpty());
    }

    @Test
    void shouldDetectInteractionBetweenWarfarinAndAspirin() {
        // Mock FDA client responses
        when(fdaClient.fetchInteractions("Warfarin"))
                .thenReturn(List.of("May cause bleeding with Aspirin"));
        when(fdaClient.fetchInteractions("Aspirin"))
                .thenReturn(List.of("Increases bleeding risk with Warfarin"));

        DrugInteractionRequest request = new DrugInteractionRequest();
        request.setDrugNames(List.of("Warfarin", "Aspirin"));

        DrugInteractionResponse response = service.checkInteractions(request);

        assertTrue(response.isSevereInteractionDetected());
        assertFalse(response.getInteractingPairs().isEmpty());
        assertTrue(response.getInteractingPairs().contains("Warfarin + Aspirin") || 
                   response.getInteractingPairs().contains("Aspirin + Warfarin"));
    }

    @Test
    void shouldFlagHighRiskDrugEvenWithoutInteraction() {
        // Mock FDA client response (no specific interaction found for single drug check context)
        when(fdaClient.fetchInteractions("Warfarin"))
                .thenReturn(List.of("General warning"));

        DrugInteractionRequest request = new DrugInteractionRequest();
        request.setDrugNames(List.of("Warfarin"));

        DrugInteractionResponse response = service.checkInteractions(request);

        // Warfarin is in HIGH_RISK_DRUGS list
        assertTrue(response.isSevereInteractionDetected());
        assertFalse(response.getWarnings().isEmpty());
    }

    @Test
    void shouldNotFlagSafeDrugs() {
        when(fdaClient.fetchInteractions("Vitamin C")).thenReturn(List.of());
        when(fdaClient.fetchInteractions("Zinc")).thenReturn(List.of());

        DrugInteractionRequest request = new DrugInteractionRequest();
        request.setDrugNames(List.of("Vitamin C", "Zinc"));

        DrugInteractionResponse response = service.checkInteractions(request);

        assertFalse(response.isSevereInteractionDetected());
        assertTrue(response.getInteractingPairs().isEmpty());
    }
}
