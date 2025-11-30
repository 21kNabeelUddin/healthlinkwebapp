package com.healthlink.infrastructure.video;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JanusService
 * 
 * Tests cover:
 * - Session creation with Janus Gateway
 * - Plugin attachment (videoroom)
 * - Room creation with configuration
 * - Error handling scenarios
 * - Transaction ID generation
 * - Network failure handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JanusService Tests")
class JanusServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private JanusService janusService;

    private static final String JANUS_URL = "http://localhost:8088/janus";

    @BeforeEach
    void setUp() {
        janusService = new JanusService(restTemplate);
        ReflectionTestUtils.setField(janusService, "janusUrl", JANUS_URL);
    }

    @Nested
    @DisplayName("Session Creation Tests")
    class SessionCreationTests {

        @Test
        @DisplayName("Should create session successfully")
        void shouldCreateSessionSuccessfully() {
            // Given
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", 12345L);

            Map<String, Object> response = new HashMap<>();
            response.put("janus", "success");
            response.put("data", responseData);

            when(restTemplate.postForObject(eq(JANUS_URL), any(Map.class), eq(Map.class)))
                    .thenReturn(response);

            // When
            Long sessionId = janusService.createSession();

            // Then
            assertThat(sessionId).isEqualTo(12345L);
            verify(restTemplate).postForObject(eq(JANUS_URL), any(Map.class), eq(Map.class));
        }

        @Test
        @DisplayName("Should throw exception when session creation fails")
        void shouldThrowExceptionWhenSessionCreationFails() {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("janus", "error");
            response.put("error", Map.of("code", 500, "reason", "Internal error"));

            when(restTemplate.postForObject(eq(JANUS_URL), any(Map.class), eq(Map.class)))
                    .thenReturn(response);

            // When / Then
            assertThatThrownBy(() -> janusService.createSession())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to create Janus session");
        }

        @Test
        @DisplayName("Should throw exception when response is null")
        void shouldThrowExceptionWhenResponseIsNull() {
            // Given
            when(restTemplate.postForObject(eq(JANUS_URL), any(Map.class), eq(Map.class)))
                    .thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> janusService.createSession())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to create Janus session");
        }

        @Test
        @DisplayName("Should include transaction ID in request")
        void shouldIncludeTransactionIdInRequest() {
            // Given
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", 12345L);

            Map<String, Object> response = new HashMap<>();
            response.put("janus", "success");
            response.put("data", responseData);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);

            when(restTemplate.postForObject(eq(JANUS_URL), requestCaptor.capture(), eq(Map.class)))
                    .thenReturn(response);

            // When
            janusService.createSession();

            // Then
            Map<String, Object> capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.get("janus")).isEqualTo("create");
            assertThat(capturedRequest.get("transaction")).isNotNull();
            assertThat(capturedRequest.get("transaction").toString()).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle network failure gracefully")
        void shouldHandleNetworkFailure() {
            // Given
            when(restTemplate.postForObject(eq(JANUS_URL), any(Map.class), eq(Map.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            // When / Then
            assertThatThrownBy(() -> janusService.createSession())
                    .isInstanceOf(RestClientException.class)
                    .hasMessageContaining("Connection refused");
        }
    }

    @Nested
    @DisplayName("Plugin Attachment Tests")
    class PluginAttachmentTests {

        @Test
        @DisplayName("Should attach videoroom plugin successfully")
        void shouldAttachVideoroomPluginSuccessfully() {
            // Given
            Long sessionId = 12345L;

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", 67890L);

            Map<String, Object> response = new HashMap<>();
            response.put("janus", "success");
            response.put("data", responseData);

            when(restTemplate.postForObject(eq(JANUS_URL + "/" + sessionId), any(Map.class), eq(Map.class)))
                    .thenReturn(response);

            // When
            Long handleId = janusService.attachPlugin(sessionId);

            // Then
            assertThat(handleId).isEqualTo(67890L);
        }

        @Test
        @DisplayName("Should use correct plugin name in request")
        void shouldUseCorrectPluginName() {
            // Given
            Long sessionId = 12345L;

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", 67890L);

            Map<String, Object> response = new HashMap<>();
            response.put("janus", "success");
            response.put("data", responseData);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);

            when(restTemplate.postForObject(eq(JANUS_URL + "/" + sessionId), requestCaptor.capture(), eq(Map.class)))
                    .thenReturn(response);

            // When
            janusService.attachPlugin(sessionId);

            // Then
            Map<String, Object> capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.get("janus")).isEqualTo("attach");
            assertThat(capturedRequest.get("plugin")).isEqualTo("janus.plugin.videoroom");
            assertThat(capturedRequest.get("transaction")).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when plugin attachment fails")
        void shouldThrowExceptionWhenPluginAttachmentFails() {
            // Given
            Long sessionId = 12345L;

            Map<String, Object> response = new HashMap<>();
            response.put("janus", "error");

            when(restTemplate.postForObject(eq(JANUS_URL + "/" + sessionId), any(Map.class), eq(Map.class)))
                    .thenReturn(response);

            // When / Then
            assertThatThrownBy(() -> janusService.attachPlugin(sessionId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to attach Janus plugin");
        }

        @Test
        @DisplayName("Should construct correct URL with session ID")
        void shouldConstructCorrectUrlWithSessionId() {
            // Given
            Long sessionId = 12345L;

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", 67890L);

            Map<String, Object> response = new HashMap<>();
            response.put("janus", "success");
            response.put("data", responseData);

            when(restTemplate.postForObject(anyString(), any(Map.class), eq(Map.class)))
                    .thenReturn(response);

            // When
            janusService.attachPlugin(sessionId);

            // Then
            verify(restTemplate).postForObject(eq(JANUS_URL + "/" + sessionId), any(Map.class), eq(Map.class));
        }
    }

    @Nested
    @DisplayName("Room Creation Tests")
    class RoomCreationTests {

        @Test
        @DisplayName("Should create room with correct parameters")
        void shouldCreateRoomWithCorrectParameters() {
            // Given
            Long sessionId = 12345L;
            Long handleId = 67890L;
            Long roomId = 99999L;
            String secret = "test-secret";

            Map<String, Object> response = new HashMap<>();
            response.put("janus", "ack");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);

            when(restTemplate.postForObject(anyString(), requestCaptor.capture(), eq(Map.class)))
                    .thenReturn(response);

            // When
            janusService.createRoom(sessionId, handleId, roomId, secret);

            // Then
            Map<String, Object> capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.get("janus")).isEqualTo("message");
            assertThat(capturedRequest.get("transaction")).isNotNull();

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) capturedRequest.get("body");
            assertThat(body.get("request")).isEqualTo("create");
            assertThat(body.get("room")).isEqualTo(roomId);
            assertThat(body.get("secret")).isEqualTo(secret);
            assertThat(body.get("publishers")).isEqualTo(2);
        }

        @Test
        @DisplayName("Should construct correct URL with session and handle ID")
        void shouldConstructCorrectUrlWithSessionAndHandleId() {
            // Given
            Long sessionId = 12345L;
            Long handleId = 67890L;
            Long roomId = 99999L;
            String secret = "test-secret";

            Map<String, Object> response = new HashMap<>();
            response.put("janus", "ack");

            when(restTemplate.postForObject(anyString(), any(Map.class), eq(Map.class)))
                    .thenReturn(response);

            // When
            janusService.createRoom(sessionId, handleId, roomId, secret);

            // Then
            String expectedUrl = JANUS_URL + "/" + sessionId + "/" + handleId;
            verify(restTemplate).postForObject(eq(expectedUrl), any(Map.class), eq(Map.class));
        }

        @Test
        @DisplayName("Should handle success response status")
        void shouldHandleSuccessResponseStatus() {
            // Given
            Long sessionId = 12345L;
            Long handleId = 67890L;
            Long roomId = 99999L;
            String secret = "test-secret";

            Map<String, Object> response = new HashMap<>();
            response.put("janus", "success");

            when(restTemplate.postForObject(anyString(), any(Map.class), eq(Map.class)))
                    .thenReturn(response);

            // When - Should not throw exception
            janusService.createRoom(sessionId, handleId, roomId, secret);

            // Then
            verify(restTemplate).postForObject(anyString(), any(Map.class), eq(Map.class));
        }

        @Test
        @DisplayName("Should handle ack response status")
        void shouldHandleAckResponseStatus() {
            // Given
            Long sessionId = 12345L;
            Long handleId = 67890L;
            Long roomId = 99999L;
            String secret = "test-secret";

            Map<String, Object> response = new HashMap<>();
            response.put("janus", "ack");

            when(restTemplate.postForObject(anyString(), any(Map.class), eq(Map.class)))
                    .thenReturn(response);

            // When - Should not throw exception
            janusService.createRoom(sessionId, handleId, roomId, secret);

            // Then
            verify(restTemplate).postForObject(anyString(), any(Map.class), eq(Map.class));
        }

        @Test
        @DisplayName("Should handle null response gracefully")
        void shouldHandleNullResponseGracefully() {
            // Given
            Long sessionId = 12345L;
            Long handleId = 67890L;
            Long roomId = 99999L;
            String secret = "test-secret";

            when(restTemplate.postForObject(anyString(), any(Map.class), eq(Map.class)))
                    .thenReturn(null);

            // When - Should not throw exception (logged as warning)
            janusService.createRoom(sessionId, handleId, roomId, secret);

            // Then
            verify(restTemplate).postForObject(anyString(), any(Map.class), eq(Map.class));
        }

        @Test
        @DisplayName("Should handle unexpected status gracefully")
        void shouldHandleUnexpectedStatusGracefully() {
            // Given
            Long sessionId = 12345L;
            Long handleId = 67890L;
            Long roomId = 99999L;
            String secret = "test-secret";

            Map<String, Object> response = new HashMap<>();
            response.put("janus", "error");
            response.put("error", Map.of("code", 436, "reason", "Room already exists"));

            when(restTemplate.postForObject(anyString(), any(Map.class), eq(Map.class)))
                    .thenReturn(response);

            // When - Should not throw exception (logged as warning)
            janusService.createRoom(sessionId, handleId, roomId, secret);

            // Then
            verify(restTemplate).postForObject(anyString(), any(Map.class), eq(Map.class));
        }
    }

    @Nested
    @DisplayName("Error Scenario Tests")
    class ErrorScenarioTests {

        @Test
        @DisplayName("Should handle timeout gracefully")
        void shouldHandleTimeoutGracefully() {
            // Given
            when(restTemplate.postForObject(eq(JANUS_URL), any(Map.class), eq(Map.class)))
                    .thenThrow(new RestClientException("Read timed out"));

            // When / Then
            assertThatThrownBy(() -> janusService.createSession())
                    .isInstanceOf(RestClientException.class)
                    .hasMessageContaining("Read timed out");
        }

        @Test
        @DisplayName("Should handle connection reset")
        void shouldHandleConnectionReset() {
            // Given
            when(restTemplate.postForObject(eq(JANUS_URL), any(Map.class), eq(Map.class)))
                    .thenThrow(new RestClientException("Connection reset"));

            // When / Then
            assertThatThrownBy(() -> janusService.createSession())
                    .isInstanceOf(RestClientException.class)
                    .hasMessageContaining("Connection reset");
        }

        @Test
        @DisplayName("Should handle invalid JSON response")
        void shouldHandleInvalidJsonResponse() {
            // Given - Response missing expected data structure
            Map<String, Object> response = new HashMap<>();
            response.put("janus", "success");
            // Missing "data" key

            when(restTemplate.postForObject(eq(JANUS_URL), any(Map.class), eq(Map.class)))
                    .thenReturn(response);

            // When / Then
            assertThatThrownBy(() -> janusService.createSession())
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle Janus server unavailable")
        void shouldHandleJanusServerUnavailable() {
            // Given
            when(restTemplate.postForObject(eq(JANUS_URL), any(Map.class), eq(Map.class)))
                    .thenThrow(new RestClientException("Connection refused: connect"));

            // When / Then
            assertThatThrownBy(() -> janusService.createSession())
                    .isInstanceOf(RestClientException.class)
                    .hasMessageContaining("Connection refused");
        }
    }

    @Nested
    @DisplayName("Transaction ID Tests")
    class TransactionIdTests {

        @Test
        @DisplayName("Should generate unique transaction IDs for each request")
        void shouldGenerateUniqueTransactionIds() {
            // Given
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", 12345L);

            Map<String, Object> response = new HashMap<>();
            response.put("janus", "success");
            response.put("data", responseData);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);

            when(restTemplate.postForObject(eq(JANUS_URL), requestCaptor.capture(), eq(Map.class)))
                    .thenReturn(response);

            // When
            janusService.createSession();
            janusService.createSession();

            // Then
            var capturedRequests = requestCaptor.getAllValues();
            assertThat(capturedRequests).hasSize(2);

            String transactionId1 = (String) capturedRequests.get(0).get("transaction");
            String transactionId2 = (String) capturedRequests.get(1).get("transaction");

            assertThat(transactionId1).isNotEqualTo(transactionId2);
        }

        @Test
        @DisplayName("Should generate valid UUID format for transaction ID")
        void shouldGenerateValidUuidForTransactionId() {
            // Given
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", 12345L);

            Map<String, Object> response = new HashMap<>();
            response.put("janus", "success");
            response.put("data", responseData);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);

            when(restTemplate.postForObject(eq(JANUS_URL), requestCaptor.capture(), eq(Map.class)))
                    .thenReturn(response);

            // When
            janusService.createSession();

            // Then
            String transactionId = (String) requestCaptor.getValue().get("transaction");
            assertThat(transactionId).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        }
    }
}
