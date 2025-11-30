package com.healthlink.infrastructure.video;

import com.healthlink.domain.consent.ConsentService;
import com.healthlink.security.model.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class ConsentEnforcementFilterTest {

    private ConsentService consentService;
    private ConsentEnforcementFilter filter;

    @BeforeEach
    void setup() {
        consentService = Mockito.mock(ConsentService.class);
        filter = new ConsentEnforcementFilter(consentService);
    }

    @Test
    void deniesWhenConsentMissing() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/v1/video-calls/session");
        when(request.getHeader("X-Client-Lang")).thenReturn("en");
        var user = buildUser();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
        when(consentService.hasAcceptedLatest(user.getId(), "en")).thenReturn(false);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
        filter.doFilterInternal(request, response, chain);
        verify(response, times(1)).setStatus(403);
        verify(chain, times(0)).doFilter(request, response);
    }

    @Test
    void allowsWhenConsentPresent() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/v1/video-calls/session");
        when(request.getHeader("X-Client-Lang")).thenReturn("en");
        var user = buildUser();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
        when(consentService.hasAcceptedLatest(user.getId(), "en")).thenReturn(true);
        filter.doFilterInternal(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }

    private CustomUserDetails buildUser() {
        var domainUser = new com.healthlink.domain.user.entity.Patient();
        domainUser.setId(UUID.randomUUID());
        domainUser.setEmail("user@example.com");
        domainUser.setPasswordHash("x");
        domainUser.setRole(com.healthlink.domain.user.enums.UserRole.PATIENT);
        domainUser.setIsActive(true);
        domainUser.setIsEmailVerified(true);
        domainUser.setApprovalStatus(com.healthlink.domain.user.enums.ApprovalStatus.APPROVED);
        return new CustomUserDetails(domainUser);
    }
}
