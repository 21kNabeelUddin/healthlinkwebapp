package com.healthlink.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Deprecated filter - Logic moved to JwtAuthenticationFilter to avoid N+1 DB calls.
 * This filter is disabled by default and only kept for backward compatibility.
 * 
 * @deprecated Use JwtAuthenticationFilter which already handles forced logout via tokensRevokedAt check.
 */
@Component
@Deprecated(forRemoval = true)
@ConditionalOnProperty(name = "healthlink.security.legacy-forced-logout-filter", havingValue = "true", matchIfMissing = false)
public class ForcedLogoutFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // No-op: pass through to the next filter
        filterChain.doFilter(request, response);
    }
}
