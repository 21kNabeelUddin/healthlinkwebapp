package com.healthlink.infrastructure.security;

import com.healthlink.infrastructure.logging.SafeLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Audit enrichment filter that captures and injects into MDC:
 * - Request ID (trace ID)
 * - Client IP address
 * - User agent
 * - Authenticated user ID
 * - Request method and path
 * 
 * These values are then available for structured logging and audit trails.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AuditEnrichmentFilter extends OncePerRequestFilter {

    private final SafeLogger log = SafeLogger.get(AuditEnrichmentFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    
    // MDC keys
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_CLIENT_IP = "clientIp";
    public static final String MDC_USER_AGENT = "userAgent";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_USERNAME = "username";
    public static final String MDC_METHOD = "method";
    public static final String MDC_PATH = "path";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Generate or extract request ID
            String requestId = extractOrGenerateRequestId(request);
            String traceId = extractOrGenerateTraceId(request);
            String clientIp = extractClientIp(request);
            String userAgent = extractUserAgent(request);
            String method = request.getMethod();
            String path = request.getRequestURI();

            // Populate MDC
            MDC.put(MDC_REQUEST_ID, requestId);
            MDC.put(MDC_TRACE_ID, traceId);
            MDC.put(MDC_CLIENT_IP, clientIp);
            MDC.put(MDC_USER_AGENT, userAgent);
            MDC.put(MDC_METHOD, method);
            MDC.put(MDC_PATH, path);

            // Add headers to response for client tracking
            response.setHeader(REQUEST_ID_HEADER, requestId);
            response.setHeader(TRACE_ID_HEADER, traceId);

            // Log request initiation (non-PHI)
            if (!isHealthCheck(path)) {
                log.event("http_request_start")
                   .with("method", method)
                   .with("path", sanitizePath(path))
                   .with("requestId", requestId)
                   .with("traceId", traceId)
                   .log();
            }

            // Continue filter chain
            filterChain.doFilter(request, response);

            // After authentication, enrich with user info
            enrichWithUserInfo();

            // Log request completion
            if (!isHealthCheck(path)) {
                log.event("http_request_complete")
                   .with("status", String.valueOf(response.getStatus()))
                   .with("requestId", requestId)
                   .log();
            }

        } finally {
            // Clear MDC to prevent thread pollution
            MDC.clear();
        }
    }

    private String extractOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }

    private String extractOrGenerateTraceId(HttpServletRequest request) {
        // Check for OpenTelemetry trace ID header
        String traceId = request.getHeader("traceparent");
        if (traceId != null && !traceId.isEmpty()) {
            // Extract trace ID from W3C traceparent format: version-traceId-spanId-flags
            String[] parts = traceId.split("-");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        
        // Fallback to custom header
        traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }

    private String extractClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For (proxy/load balancer)
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            // Take first IP (client)
            return xff.split(",")[0].trim();
        }

        // Check X-Real-IP
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }
        // Truncate to prevent log bloat
        return userAgent.length() > 200 ? userAgent.substring(0, 200) + "..." : userAgent;
    }

    private void enrichWithUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            MDC.put(MDC_USERNAME, username);
            
            // If principal contains user ID (custom UserDetails), extract it
            Object principal = auth.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                // Assuming custom UserDetails has getUserId() method
                try {
                    var method = userDetails.getClass().getMethod("getUserId");
                    Object userId = method.invoke(userDetails);
                    if (userId != null) {
                        MDC.put(MDC_USER_ID, userId.toString());
                    }
                } catch (Exception e) {
                    // Method not found, skip
                }
            }
        }
    }

    private String sanitizePath(String path) {
        // Remove UUIDs and IDs from path for logging (avoid PHI in logs)
        return path.replaceAll("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "<UUID>")
                   .replaceAll("/\\d+", "/<ID>");
    }

    private boolean isHealthCheck(String path) {
        return path.startsWith("/actuator/health") || path.startsWith("/actuator/prometheus");
    }
}
