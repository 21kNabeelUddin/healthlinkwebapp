package com.healthlink.controller;

import com.healthlink.dto.ResponseEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/cache")
@RequiredArgsConstructor
public class CacheEvictionController {
    private final CacheManager cacheManager;

    @DeleteMapping("/medical-records/{patientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEnvelope<String> evictMedicalRecords(@PathVariable UUID patientId) {
        var cache = cacheManager.getCache("medicalRecords");
        if (cache != null) cache.evict(patientId);
        return ResponseEnvelope.<String>builder()
                .data("EVICTED_MEDICAL_RECORDS:" + patientId)
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId("cache-evict-medrec")
                .build();
    }

    @DeleteMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEnvelope<String> evictAll() {
        cacheManager.getCacheNames().forEach(name -> {
            var c = cacheManager.getCache(name);
            if (c != null) c.clear();
        });
        return ResponseEnvelope.<String>builder()
                .data("EVICTED_ALL")
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId("cache-evict-all")
                .build();
    }
}
