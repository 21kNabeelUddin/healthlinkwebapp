package com.healthlink.controller.advice;

import com.healthlink.dto.ResponseEnvelope;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class ResponseEnvelopeAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends org.springframework.http.converter.HttpMessageConverter<?>> converterType) {
        // Don't wrap if already ResponseEnvelope
        return !ResponseEnvelope.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends org.springframework.http.converter.HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        // Skip wrapping for actuator endpoints
        String path = request.getURI().getPath();
        if (path != null && (path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/api-docs"))) {
            return body;
        }
        
        // Skip if already wrapped
        if (body instanceof ResponseEnvelope<?>) {
            return body;
        }
        
        // Skip for String converter (prevents ClassCastException)
        if (body instanceof String) {
            return body;
        }
        
        String traceId = request.getHeaders().getFirst("X-Trace-Id");
        if (traceId == null) traceId = "auto";
        return ResponseEnvelope.builder()
                .data(body)
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId(traceId)
                .build();
    }
}
