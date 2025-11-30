package com.healthlink.dto.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Centralized mapper utilities for domain -> DTO conversions to ensure
 * consistent timestamp handling and future cross-cutting transformations.
 */
public final class DomainDtoMapper {
    private DomainDtoMapper() {}

    public static OffsetDateTime toUtc(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atOffset(ZoneOffset.UTC);
    }
}
