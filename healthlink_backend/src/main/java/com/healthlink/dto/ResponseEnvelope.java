package com.healthlink.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponseEnvelope<T> {
    private final T data;
    private final Meta meta;
    private final String traceId;
    private final Error error;

    @Getter
    @Builder
    public static class Meta {
        private final String version;
    }

    @Getter
    @Builder
    public static class Error {
        private final String code; // Machine-readable error code
        private final String message; // Human-readable message
        private final String details; // Optional details for debugging
    }
}
