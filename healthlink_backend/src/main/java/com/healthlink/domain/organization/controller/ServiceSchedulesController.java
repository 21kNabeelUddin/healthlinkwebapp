package com.healthlink.domain.organization.controller;

import com.healthlink.domain.organization.dto.ServiceScheduleRequest;
import com.healthlink.domain.organization.dto.ServiceScheduleResponse;
import com.healthlink.domain.organization.service.ServiceScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-schedules")
@RequiredArgsConstructor
public class ServiceSchedulesController {

    private final ServiceScheduleService scheduleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ORGANIZATION','ADMIN')")
    public ServiceScheduleResponse create(@Valid @RequestBody ServiceScheduleRequest request) {
        return scheduleService.create(request);
    }

    @GetMapping("/offering/{offeringId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ORGANIZATION','ADMIN','STAFF','PATIENT')")
    public List<ServiceScheduleResponse> list(@PathVariable UUID offeringId) {
        return scheduleService.list(offeringId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ORGANIZATION','ADMIN')")
    public void delete(@PathVariable UUID id) {
        scheduleService.delete(id);
    }
}