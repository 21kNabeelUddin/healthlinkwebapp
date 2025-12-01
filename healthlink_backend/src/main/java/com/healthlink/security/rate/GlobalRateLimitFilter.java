package com.healthlink.security.rate;

import com.healthlink.infrastructure.logging.SafeLogger;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Global rate limiting filter using Bucket4j with Redis backing.
 * Enforces role-based rate limits on all API endpoints.
 * Only active when healthlink.rate-limit.enabled=true.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "healthlink.rate-limit.enabled", havingValue = "true", matchIfMissing = false)
public class GlobalRateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;
    private final SafeLogger log = SafeLogger.get(GlobalRateLimitFilter.class);

    // Rate limits per role per minute
    private static final long PATIENT_LIMIT = 60;
    private static final long DOCTOR_LIMIT = 120;
    private static final long STAFF_LIMIT = 100;
    private static final long ORGANIZATION_LIMIT = 200;
    private static final long ADMIN_LIMIT = 300;
    private static final long ANONYMOUS_LIMIT = 20;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip rate limiting for actuator endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey = resolveBucketKey(request);
        long limit = resolveLimit(request);

        Bucket bucket = proxyManager.builder().build(bucketKey, getConfigSupplier(limit));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.event("rate_limit_exceeded")
                    .withMasked("bucketKey", bucketKey)
                    .with("path", path)
                    .log();
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\",\"code\":\"RATE_LIMIT_EXCEEDED\"}");
        }
    }

    private String resolveBucketKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse("UNKNOWN");
            return "rate:limit:" + role + ":" + username;
        }
        // Anonymous requests limited by IP
        String ip = getClientIp(request);
        return "rate:limit:ANONYMOUS:" + ip;
    }

    private long resolveLimit(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse("ROLE_PATIENT");
            return switch (role) {
                case "ROLE_ADMIN", "ROLE_PLATFORM_OWNER" -> ADMIN_LIMIT;
                case "ROLE_ORGANIZATION" -> ORGANIZATION_LIMIT;
                case "ROLE_DOCTOR" -> DOCTOR_LIMIT;
                case "ROLE_STAFF" -> STAFF_LIMIT;
                default -> PATIENT_LIMIT;
            };
        }
        return ANONYMOUS_LIMIT;
    }

    private Supplier<BucketConfiguration> getConfigSupplier(long limit) {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limit)
                        .refillIntervally(limit, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
