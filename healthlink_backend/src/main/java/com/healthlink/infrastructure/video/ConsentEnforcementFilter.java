package com.healthlink.infrastructure.video;

import com.healthlink.domain.consent.ConsentService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.healthlink.security.model.CustomUserDetails;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ConsentEnforcementFilter extends OncePerRequestFilter {

    private final ConsentService consentService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/video-calls")) {
            String lang = request.getHeader("X-Client-Lang");
            if (lang == null || lang.isBlank()) {
                lang = "en"; // default
            }
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
                if (!consentService.hasAcceptedLatest(cud.getId(), lang)) {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.getWriter().write("Consent required: please accept latest version");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}