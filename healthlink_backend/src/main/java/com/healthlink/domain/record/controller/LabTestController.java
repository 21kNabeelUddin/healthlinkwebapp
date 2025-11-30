package com.healthlink.domain.record.controller;

import com.healthlink.domain.record.dto.LabOrderResponse; // Reuse existing DTO until rename migration applied
import com.healthlink.domain.record.service.LabOrderService;
import com.healthlink.security.annotation.PhiAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Alias controller exposing lab order functionality under /lab-tests path to align spec terminology.
 * Keeps backward compatibility with existing /lab-orders endpoints.
 */
@RestController
@RequestMapping("/api/v1/lab-tests")
@Tag(name = "Lab Tests", description = "Alias endpoints for lab test orders")
@RequiredArgsConstructor
public class LabTestController {

    private final LabOrderService labOrderService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Create a lab test order for a patient (alias)")
    @PhiAccess(reason = "lab_test_order_create", entityType = LabOrderResponse.class, idParam = "patientId")
    public LabOrderResponse create(@RequestParam UUID patientId,
                                   @RequestParam String orderName,
                                   @RequestParam(required = false) String description) {
        return labOrderService.createLabOrder(patientId, orderName, description);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR','STAFF') or (hasRole('PATIENT') and principal.id == #patientId)")
    @Operation(summary = "List lab test orders for a patient (alias)")
    @PhiAccess(reason = "lab_test_list", entityType = LabOrderResponse.class, idParam = "patientId")
    public List<LabOrderResponse> forPatient(@PathVariable UUID patientId) {
        return labOrderService.getLabOrdersByPatient(patientId);
    }

    @PostMapping("/{id}/attach-result")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @Operation(summary = "Attach a lab result URL to an existing test order (alias)")
    @PhiAccess(reason = "lab_test_result_attach", entityType = LabOrderResponse.class, idParam = "id")
    public LabOrderResponse attachResult(@PathVariable UUID id,
                                         @RequestParam String resultUrl) {
        return labOrderService.attachResult(id, resultUrl);
    }
}
