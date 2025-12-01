package com.healthlink.controller;

import com.healthlink.domain.search.dto.DoctorSearchRequest;
import com.healthlink.domain.search.service.DoctorSearchService;
import com.healthlink.dto.ResponseEnvelope;
import com.healthlink.search.SearchIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.data.elasticsearch.repositories", name = "enabled", havingValue = "true", matchIfMissing = false)
public class SearchController {
    private final SearchIndexService searchIndexService;
    private final DoctorSearchService doctorSearchService;

    @GetMapping("/doctors")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEnvelope<java.util.List<DoctorSearchResponse>> searchDoctors(@RequestParam String query) {
        var request = DoctorSearchRequest.builder().query(query).build();
        var doctors = doctorSearchService.searchDoctors(request);
        var results = doctors.stream()
                .map(d -> new DoctorSearchResponse(UUID.fromString(d.getId()), d.getName(), d.getSpecialty(), d.getAverageRating()))
                .toList();
        return ResponseEnvelope.<java.util.List<DoctorSearchResponse>>builder()
                .data(results)
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId("search-doctors-exec")
                .build();
    }

    public record DoctorSearchResponse(UUID id, String fullName, String specialization, Double rating) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEnvelope<java.util.List<com.healthlink.search.SearchIndexService.SearchHit>> search(Authentication auth,
                                                                                                       @RequestParam String q,
                                                                                                       @RequestParam(required=false, defaultValue="false") boolean includeLab) {
        // Scope: patient can only search own; doctor/admin must provide patientId (future enhancement)
        UUID patientScope = resolvePatientScope(auth);
        var results = searchIndexService.search(q, patientScope, includeLab);
        return ResponseEnvelope.<java.util.List<com.healthlink.search.SearchIndexService.SearchHit>>builder()
                .data(results)
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId("search-exec")
                .build();
    }

    private UUID resolvePatientScope(Authentication auth) {
        // For now: if PATIENT role, derive from principal details when available; otherwise throw for doctor/admin
        boolean isPatient = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient && auth.getPrincipal() instanceof com.healthlink.security.model.CustomUserDetails cud) {
            return cud.getId();
        }
        throw new RuntimeException("Patient scope required for search (doctor/admin scoping not yet implemented)");
    }
}
