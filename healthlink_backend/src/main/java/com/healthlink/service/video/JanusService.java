package com.healthlink.service.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Janus WebRTC Gateway Service
 * Integrates with self-hosted Janus server for video consultations
 * Reference: https://janus.conf.meetecho.com/docs/
 */
@Service("legacyJanusService")
@ConditionalOnProperty(value = "healthlink.legacy.janus.enabled", havingValue = "true")
@Deprecated
@Slf4j
public class JanusService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${healthlink.janus.api-url}")
    private String janusApiUrl;

    @Value("${healthlink.janus.api-secret:#{null}}")
    private String janusApiSecret;

    @Value("${healthlink.janus.admin-key:#{null}}")
    private String janusAdminKey;

    private static final String VIDEOROOM_PLUGIN = "janus.plugin.videoroom";

    public JanusService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Create a new video room for an appointment
     */
    public JanusRoom createVideoRoom(String appointmentId, int maxParticipants) {
        try {
            log.info("Creating Janus video room for appointment: {}", appointmentId);

            // Create session
            Long sessionId = createSession();
            if (sessionId == null) {
                throw new RuntimeException("Failed to create Janus session");
            }

            // Attach to videoroom plugin
            Long handleId = attachPlugin(sessionId, VIDEOROOM_PLUGIN);
            if (handleId == null) {
                throw new RuntimeException("Failed to attach to videoroom plugin");
            }

            // Create room
            ObjectNode createRoomRequest = objectMapper.createObjectNode();
            createRoomRequest.put("request", "create");
            createRoomRequest.put("room", generateRoomId());
            createRoomRequest.put("description", "Appointment: " + appointmentId);
            createRoomRequest.put("publishers", maxParticipants);
            createRoomRequest.put("audiocodec", "opus");
            createRoomRequest.put("videocodec", "vp8");
            createRoomRequest.put("record", false); // Recording disabled by default
            createRoomRequest.put("permanent", false); // Temporary room

            ObjectNode messageRequest = objectMapper.createObjectNode();
            messageRequest.put("janus", "message");
            messageRequest.put("transaction", UUID.randomUUID().toString());
            messageRequest.set("body", createRoomRequest);

            String url = String.format("%s/%d/%d", janusApiUrl, sessionId, handleId);
            ResponseEntity<String> response = sendRequest(url, messageRequest);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode result = objectMapper.readTree(response.getBody());
                Long roomId = result.get("plugindata").get("data").get("room").asLong();

                JanusRoom room = new JanusRoom();
                room.setSessionId(sessionId);
                room.setHandleId(handleId);
                room.setRoomId(roomId);
                room.setAppointmentId(appointmentId);

                log.info("Created Janus room: {} for appointment: {}", roomId, appointmentId);
                return room;
            } else {
                throw new RuntimeException("Failed to create Janus room");
            }

        } catch (Exception e) {
            log.error("Error creating Janus video room", e);
            throw new RuntimeException("Failed to create video room", e);
        }
    }

    /**
     * Destroy video room
     */
    public void destroyVideoRoom(Long sessionId, Long handleId, Long roomId) {
        try {
            log.info("Destroying Janus room: {}", roomId);

            ObjectNode destroyRequest = objectMapper.createObjectNode();
            destroyRequest.put("request", "destroy");
            destroyRequest.put("room", roomId);

            ObjectNode messageRequest = objectMapper.createObjectNode();
            messageRequest.put("janus", "message");
            messageRequest.put("transaction", UUID.randomUUID().toString());
            messageRequest.set("body", destroyRequest);

            String url = String.format("%s/%d/%d", janusApiUrl, sessionId, handleId);
            sendRequest(url, messageRequest);

            log.info("Destroyed Janus room: {}", roomId);

        } catch (Exception e) {
            log.error("Error destroying Janus room", e);
        }
    }

    /**
     * Create Janus session
     */
    private Long createSession() {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("janus", "create");
            request.put("transaction", UUID.randomUUID().toString());

            ResponseEntity<String> response = sendRequest(janusApiUrl, request);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode result = objectMapper.readTree(response.getBody());
                return result.get("data").get("id").asLong();
            }
        } catch (Exception e) {
            log.error("Failed to create Janus session", e);
        }
        return null;
    }

    /**
     * Attach to Janus plugin
     */
    private Long attachPlugin(Long sessionId, String plugin) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("janus", "attach");
            request.put("plugin", plugin);
            request.put("transaction", UUID.randomUUID().toString());

            String url = String.format("%s/%d", janusApiUrl, sessionId);
            ResponseEntity<String> response = sendRequest(url, request);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode result = objectMapper.readTree(response.getBody());
                return result.get("data").get("id").asLong();
            }
        } catch (Exception e) {
            log.error("Failed to attach to Janus plugin", e);
        }
        return null;
    }

    /**
     * Send HTTP request to Janus
     */
    private ResponseEntity<String> sendRequest(String url, ObjectNode body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (janusApiSecret != null && !janusApiSecret.isEmpty()) {
                body.put("apisecret", janusApiSecret);
            }

            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
            return restTemplate.postForEntity(url, request, String.class);

        } catch (Exception e) {
            log.error("Error sending request to Janus", e);
            throw new RuntimeException("Janus API request failed", e);
        }
    }

    /**
     * Generate random room ID
     */
    private Long generateRoomId() {
        return (long) (Math.random() * 1000000000);
    }

    /**
     * Janus Room DTO
     */
    @lombok.Data
    public static class JanusRoom {
        private Long sessionId;
        private Long handleId;
        private Long roomId;
        private String appointmentId;
    }
}
