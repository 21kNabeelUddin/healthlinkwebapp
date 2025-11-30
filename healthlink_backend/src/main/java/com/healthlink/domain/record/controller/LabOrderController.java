package com.healthlink.domain.record.controller;

import com.healthlink.domain.record.dto.LabOrderResponse;
import com.healthlink.domain.record.service.LabOrderService;
import com.healthlink.security.annotation.PhiAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lab-orders")
@Tag(name = "Lab Orders", description = "Lab test order and result management")
@RequiredArgsConstructor
public class LabOrderController {

    private final LabOrderService labOrderService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Create a lab test order for a patient")
    @PhiAccess(reason = "lab_order_create", entityType = LabOrderResponse.class, idParam = "patientId")
    public LabOrderResponse create(@RequestParam UUID patientId,
                                   @RequestParam String orderName,
                                   @RequestParam(required = false) String description) {
        return labOrderService.createLabOrder(patientId, orderName, description);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR','STAFF') or (hasRole('PATIENT') and principal.id == #patientId)")
    @Operation(summary = "List lab orders for a patient")
    public List<LabOrderResponse> forPatient(@PathVariable UUID patientId) {
        return labOrderService.getLabOrdersByPatient(patientId);
    }

    @PostMapping("/{id}/attach-result")
    @PreAuthorize("hasRole('DOCTOR') or (hasRole('PATIENT') and @ownershipGuard.isLabOrderOwner(#id))")
    @Operation(summary = "Attach a lab result URL to an existing order")
    @PhiAccess(reason = "lab_result_attach", entityType = LabOrderResponse.class, idParam = "id")
    public LabOrderResponse attachResult(@PathVariable UUID id,
                                         @RequestParam String resultUrl) {
        return labOrderService.attachResult(id, resultUrl);
    }
}
