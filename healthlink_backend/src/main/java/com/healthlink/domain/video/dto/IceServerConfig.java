package com.healthlink.domain.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ICE server configuration for WebRTC peer connections
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IceServerConfig {

    /**
     * List of STUN/TURN server URLs
     * Examples:
     * - STUN: "stun:stun.l.google.com:19302"
     * - TURN: "turn:turn.example.com:3478"
     */
    private List<String> urls;

    /**
     * Username for TURN server authentication (optional, TURN only)
     */
    private String username;

    /**
     * Credential for TURN server authentication (optional, TURN only)
     */
    private String credential;
}
