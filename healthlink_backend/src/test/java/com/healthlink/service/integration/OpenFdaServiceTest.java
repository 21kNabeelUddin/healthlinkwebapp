package com.healthlink.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for OpenFdaService
 */
@ExtendWith(MockitoExtension.class)
class OpenFdaServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OpenFdaService openFdaService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(openFdaService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(openFdaService, "openFdaApiUrl", "https://api.fda.gov");
        ReflectionTestUtils.setField(openFdaService, "apiKey", null);
    }

    @Test
    void getDrugInfo_shouldReturnDrugInfoForValidDrug() {
        String mockResponse = """
                {
                  "results": [{
                    "openfda": {
                      "brand_name": ["Aspirin"],
                      "generic_name": ["acetylsalicylic acid"]
                    },
                    "indications_and_usage": ["Pain relief"],
                    "warnings": ["May cause stomach bleeding. Avoid with blood thinners."]
                  }]
                }
                """;

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        OpenFdaService.DrugInfo result = openFdaService.getDrugInfo("Aspirin");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Aspirin");
        assertThat(result.getGenericName()).isEqualTo("acetylsalicylic acid");
        assertThat(result.getIndications()).isEqualTo("Pain relief");
        assertThat(result.getInteractions()).isNotEmpty();
    }

    @Test
    void getDrugInfo_shouldReturnNullForNonExistentDrug() {
        String mockResponse = """
                {
                  "results": []
                }
                """;

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        OpenFdaService.DrugInfo result = openFdaService.getDrugInfo("NonExistentDrug");

        assertThat(result).isNull();
    }

    @Test
    void getDrugInfo_shouldHandleApiError() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        OpenFdaService.DrugInfo result = openFdaService.getDrugInfo("Aspirin");

        assertThat(result).isNull();
    }

    @Test
    void checkDrugInteractions_shouldIdentifyInteractions() {
        String mockResponse1 = """
                {
                  "results": [{
                    "openfda": {
                      "brand_name": ["Aspirin"],
                      "generic_name": ["acetylsalicylic acid"]
                    },
                    "warnings": ["Avoid with other NSAIDs. May interact with warfarin."]
                  }]
                }
                """;

        String mockResponse2 = """
                {
                  "results": [{
                    "openfda": {
                      "brand_name": ["Ibuprofen"],
                      "generic_name": ["ibuprofen"]
                    },
                    "warnings": ["Do not combine with aspirin or other blood thinners."]
                  }]
                }
                """;

        when(restTemplate.getForEntity(contains("Aspirin"), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockResponse1, HttpStatus.OK));
        when(restTemplate.getForEntity(contains("Ibuprofen"), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockResponse2, HttpStatus.OK));

        List<String> drugs = Arrays.asList("Aspirin", "Ibuprofen");
        OpenFdaService.DrugInteractionResult result = openFdaService.checkDrugInteractions(drugs);

        assertThat(result).isNotNull();
        assertThat(result.getDrugs()).containsExactlyInAnyOrder("Aspirin", "Ibuprofen");
        assertThat(result.isHasInteractions()).isTrue();
        assertThat(result.getInteractions()).isNotEmpty();
    }

    @Test
    void checkDrugInteractions_shouldReturnNoInteractionsForSafeDrugs() {
        String mockResponse = """
                {
                  "results": [{
                    "openfda": {
                      "brand_name": ["Vitamin C"],
                      "generic_name": ["ascorbic acid"]
                    },
                    "warnings": ["Generally safe"]
                  }]
                }
                """;

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<String> drugs = Arrays.asList("Vitamin C");
        OpenFdaService.DrugInteractionResult result = openFdaService.checkDrugInteractions(drugs);

        assertThat(result).isNotNull();
        assertThat(result.isHasInteractions()).isFalse();
        assertThat(result.getInteractions()).isEmpty();
    }

    @Test
    void getDrugInfo_shouldExtractInteractionsFromWarnings() {
        String mockResponse = """
                {
                  "results": [{
                    "openfda": {
                      "brand_name": ["Warfarin"],
                      "generic_name": ["warfarin sodium"]
                    },
                    "warnings": [
                      "Drug interaction: avoid with aspirin",
                      "May cause bleeding",
                      "Interaction with NSAIDs possible"
                    ]
                  }]
                }
                """;

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        OpenFdaService.DrugInfo result = openFdaService.getDrugInfo("Warfarin");

        assertThat(result).isNotNull();
        assertThat(result.getInteractions()).hasSize(2); // Only warnings with "interaction" or "avoid"
    }
}
