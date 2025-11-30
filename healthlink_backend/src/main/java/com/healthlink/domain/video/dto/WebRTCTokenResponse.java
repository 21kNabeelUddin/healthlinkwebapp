package com.healthlink.domain.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * WebRTC token response containing authentication token and ICE server
 * configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebRTCTokenResponse {

    /**
     * JWT token for WebRTC signaling authentication
     */
    private String token;

    /**
     * WebSocket URL for signaling server
     */
    private String websocketUrl;

    /**
     * Token expiration timestamp (ISO-8601)
     */
    private String expiresAt;

    /**
     * ICE servers configuration for STUN/TURN
     */
    private List<IceServerConfig> iceServers;
}
