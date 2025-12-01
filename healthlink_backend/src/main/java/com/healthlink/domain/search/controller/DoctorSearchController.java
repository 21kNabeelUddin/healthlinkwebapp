package com.healthlink.domain.search.controller;

import com.healthlink.domain.search.dto.DoctorProfileResponse;
import com.healthlink.domain.search.dto.DoctorSearchRequest;
import com.healthlink.domain.search.dto.DoctorSearchResponse;
import com.healthlink.domain.search.service.DoctorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Doctor Search REST Controller
 * Provides Elasticsearch-based doctor discovery
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Doctor Search", description = "Search and discover doctors")
@ConditionalOnProperty(prefix = "spring.data.elasticsearch.repositories", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DoctorSearchController {

    private final DoctorSearchService searchService;

    /**
     * Search doctors with filters
     * Filters: specialty, location (city/area), rating, availability
     * Sort: rating (default), experience, fee
     */
    @PostMapping("/doctors")
    @Operation(summary = "Search doctors with filters")
    public ResponseEntity<List<DoctorSearchResponse>> searchDoctors(@RequestBody DoctorSearchRequest request) {
        return ResponseEntity.ok(searchService.searchDoctors(request));
    }

    /**
     * Quick search by specialty
     */
    @GetMapping("/doctors/specialty/{specialty}")
    @Operation(summary = "Search doctors by specialty")
    public ResponseEntity<List<DoctorSearchResponse>> searchBySpecialty(@PathVariable String specialty) {
        DoctorSearchRequest request = DoctorSearchRequest.builder()
                .specialty(specialty)
                .sortBy("rating")
                .build();
        return ResponseEntity.ok(searchService.searchDoctors(request));
    }

    /**
     * Quick search by city
     */
    @GetMapping("/doctors/city/{city}")
    @Operation(summary = "Search doctors by city")
    public ResponseEntity<List<DoctorSearchResponse>> searchByCity(@PathVariable String city) {
        DoctorSearchRequest request = DoctorSearchRequest.builder()
                .city(city)
                .sortBy("rating")
                .build();
        return ResponseEntity.ok(searchService.searchDoctors(request));
    }

    /**
     * Detailed doctor profile for patient view
     */
    @GetMapping("/doctors/{doctorId}")
    @Operation(summary = "Get doctor profile")
    public ResponseEntity<DoctorProfileResponse> getDoctorProfile(@PathVariable String doctorId) {
        return ResponseEntity.ok(searchService.getDoctorProfile(doctorId));
    }
}
