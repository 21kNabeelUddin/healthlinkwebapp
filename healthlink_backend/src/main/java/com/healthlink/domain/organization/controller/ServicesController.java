package com.healthlink.domain.organization.controller;

import com.healthlink.domain.organization.dto.ServiceOfferingRequest;
import com.healthlink.domain.organization.dto.ServiceOfferingResponse;
import com.healthlink.domain.organization.service.ServiceOfferingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServicesController {

    private final ServiceOfferingService offeringService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ORGANIZATION','ADMIN')")
    public ServiceOfferingResponse create(@Valid @RequestBody ServiceOfferingRequest request) {
        return offeringService.create(request);
    }

    @GetMapping("/facility/{facilityId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ORGANIZATION','ADMIN','STAFF')")
    public List<ServiceOfferingResponse> list(@PathVariable UUID facilityId) {
        return offeringService.list(facilityId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ORGANIZATION','ADMIN')")
    public ServiceOfferingResponse update(@PathVariable UUID id,
                                          @Valid @RequestBody ServiceOfferingRequest request) {
        return offeringService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ORGANIZATION','ADMIN')")
    public void delete(@PathVariable UUID id) {
        offeringService.delete(id);
    }
}