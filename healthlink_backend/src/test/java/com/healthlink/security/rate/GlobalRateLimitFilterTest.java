package com.healthlink.security.rate;

import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalRateLimitFilterTest {

    @Mock private ProxyManager<String> proxyManager;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @Mock private BucketProxy bucket;
    @Mock private RemoteBucketBuilder<String> bucketBuilder;
    @Mock private PrintWriter writer;

    private GlobalRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GlobalRateLimitFilter(proxyManager);
        SecurityContextHolder.clearContext();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAllowRequestWhenBucketHasTokens() throws Exception {
        // Mock Bucket4j
        when(proxyManager.builder()).thenReturn(bucketBuilder);
        when(bucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        // Mock Request
        when(request.getRequestURI()).thenReturn("/api/v1/patients");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldBlockRequestWhenBucketExhausted() throws Exception {
        // Mock Bucket4j
        when(proxyManager.builder()).thenReturn(bucketBuilder);
        when(bucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        // Mock Request
        when(request.getRequestURI()).thenReturn("/api/v1/patients");
        when(response.getWriter()).thenReturn(writer);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(429);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    void shouldApplyRoleBasedLimits() throws Exception {
        // Setup Security Context
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("user", "pass", 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR")))
        );

        when(proxyManager.builder()).thenReturn(bucketBuilder);
        when(bucketBuilder.build(eq("rate:limit:ROLE_DOCTOR:user"), any(Supplier.class))).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);
        when(request.getRequestURI()).thenReturn("/api/v1/doctors/appointments");

        filter.doFilterInternal(request, response, filterChain);
        
        verify(bucketBuilder).build(eq("rate:limit:ROLE_DOCTOR:user"), any(Supplier.class));
    }
}
