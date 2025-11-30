package com.healthlink.service.video;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for JanusService
 */
@ExtendWith(MockitoExtension.class)
class JanusServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    @SuppressWarnings("deprecation") // JanusService is deprecated but still used in tests
    private JanusService janusService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(janusService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(janusService, "janusApiUrl", "http://localhost:8088/janus");
        ReflectionTestUtils.setField(janusService, "janusApiSecret", "test-secret");
    }

    @Test
    void createVideoRoom_shouldCreateSessionAndRoom() {
        String createSessionResponse = "{\"janus\":\"success\",\"transaction\":\"abc\",\"data\":{\"id\":123}}";
        String attachPluginResponse = "{\"janus\":\"success\",\"transaction\":\"def\",\"data\":{\"id\":456}}";
        String createRoomResponse = "{\"janus\":\"success\",\"plugindata\":{\"data\":{\"room\":789}}}";

        when(restTemplate.postForEntity(eq("http://localhost:8088/janus"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(createSessionResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(eq("http://localhost:8088/janus/123"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(attachPluginResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(eq("http://localhost:8088/janus/123/456"), any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(new ResponseEntity<>(createRoomResponse, HttpStatus.OK));

        @SuppressWarnings("deprecation")
        JanusService.JanusRoom result = janusService.createVideoRoom("appt-123", 2);

        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(123L);
        assertThat(result.getHandleId()).isEqualTo(456L);
        assertThat(result.getRoomId()).isEqualTo(789L);
        assertThat(result.getAppointmentId()).isEqualTo("appt-123");
    }

    @Test
    void createVideoRoom_shouldThrowWhenSessionCreationFails() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> janusService.createVideoRoom("appt-123", 2))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create video room");
    }

    @Test
    void destroyVideoRoom_shouldSendDestroyRequest() {
        String destroyResponse = "{\"janus\":\"success\"}";

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(destroyResponse, HttpStatus.OK));

        assertThatCode(() -> janusService.destroyVideoRoom(123L, 456L, 789L))
                .doesNotThrowAnyException();

        verify(restTemplate).postForEntity(
                eq("http://localhost:8088/janus/123/456"),
                any(HttpEntity.class),
                eq(String.class));
    }

    @Test
    void destroyVideoRoom_shouldHandleErrorsGracefully() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        assertThatCode(() -> janusService.destroyVideoRoom(123L, 456L, 789L))
                .doesNotThrowAnyException();
    }
}
