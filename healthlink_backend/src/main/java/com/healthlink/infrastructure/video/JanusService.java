package com.healthlink.infrastructure.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.healthlink.infrastructure.logging.SafeLogger;

@Service
@Primary
public class JanusService {
    private static final SafeLogger log = SafeLogger.getLogger(JanusService.class);

    @Value("${healthlink.webrtc.janus.url:http://janus:8088/janus}")
    private String janusUrl;

    private final RestTemplate restTemplate;
    
    public JanusService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public Long createSession() {
        long start = System.nanoTime();
        Map<String, Object> request = new HashMap<>();
        request.put("janus", "create");
        request.put("transaction", generateTransactionId());
        Map<String, Object> response = restTemplate.postForObject(janusUrl, request, Map.class);
        
        if (response != null && "success".equals(response.get("janus"))) {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            log.info("Janus session created in {} ms", String.valueOf(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
            return ((Number) data.get("id")).longValue();
        }
        log.warn("Failed to create Janus session");
        throw new RuntimeException("Failed to create Janus session");
    }

    @SuppressWarnings("unchecked")
    public Long attachPlugin(Long sessionId) {
        long start = System.nanoTime();
        String url = janusUrl + "/" + sessionId;
        Map<String, Object> request = new HashMap<>();
        request.put("janus", "attach");
        request.put("plugin", "janus.plugin.videoroom");
        request.put("transaction", generateTransactionId());
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        if (response != null && "success".equals(response.get("janus"))) {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            log.info("Janus plugin attached in {} ms", String.valueOf(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
            return ((Number) data.get("id")).longValue();
        }
        log.warn("Failed to attach Janus plugin");
        throw new RuntimeException("Failed to attach Janus plugin");
    }

    public void createRoom(Long sessionId, Long handleId, Long roomId, String secret) {
        String url = janusUrl + "/" + sessionId + "/" + handleId;
        
        Map<String, Object> body = new HashMap<>();
        body.put("request", "create");
        body.put("room", roomId);
        body.put("secret", secret);
        body.put("publishers", 2); // Doctor + Patient

        Map<String, Object> request = new HashMap<>();
        request.put("janus", "message");
        request.put("body", body);
        request.put("transaction", generateTransactionId());

        @SuppressWarnings("unchecked") Map<String,Object> response = restTemplate.postForObject(url, request, Map.class);
        if (response == null) {
            log.warn("Janus room creation no response for room {}", String.valueOf(roomId));
        } else if (!"ack".equals(response.get("janus")) && !"success".equals(response.get("janus"))) {
            log.warn("Janus room creation unexpected status {} for room {}", String.valueOf(response.get("janus")), String.valueOf(roomId));
        } else {
            log.info("Janus room creation requested for room {}", String.valueOf(roomId));
        }
    }

    private String generateTransactionId() {
        return java.util.UUID.randomUUID().toString();
    }
}
