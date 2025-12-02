package com.healthlink.security.jwt;

import com.healthlink.infrastructure.logging.SafeLogger;
import com.healthlink.security.service.CustomUserDetailsService;
import com.healthlink.security.token.AccessTokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
import com.healthlink.security.model.CustomUserDetails;
import io.jsonwebtoken.Claims;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    // Removed direct UserRepository dependency to avoid N+1
    private final AccessTokenBlacklistService accessTokenBlacklistService;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        
        // Skip if no auth header or doesn't start with Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Extract JWT token
            jwt = authHeader.substring(7);
            userEmail = jwtService.extractUsername(jwt);
            
            // If email is valid and user is not already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                
                // Validate token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Per-token revocation check (JTI blacklist) - fault-tolerant (service handles Redis failures)
                    String jti = jwtService.extractJti(jwt);
                    if (accessTokenBlacklistService.isBlacklisted(jti)) {
                        filterChain.doFilter(request, response);
                        return; // token explicitly revoked
                    }
                    
                    // Forced Logout Check (User-level revocation)
                    if (userDetails instanceof CustomUserDetails customUser) {
                        if (customUser.getTokensRevokedAt() != null) {
                            Date issuedAt = jwtService.extractClaim(jwt, Claims::getIssuedAt);
                            if (issuedAt != null && issuedAt.toInstant().isBefore(customUser.getTokensRevokedAt())) {
                                SafeLogger.get(JwtAuthenticationFilter.class)
                                    .event("token_revoked")
                                    .with("issuedAt", issuedAt.toInstant().toString())
                                    .with("revokedAt", customUser.getTokensRevokedAt().toString())
                                    .withMasked("email", userEmail)
                                    .log();
                                // Do not authenticate
                                filterChain.doFilter(request, response);
                                return;
                            }
                        }
                    }
                    
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("User {} authenticated successfully", userEmail);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}
