package com.healthlink.domain.record.controller;

import com.healthlink.domain.record.dto.PrescriptionRequest;
import com.healthlink.domain.record.dto.PrescriptionResponse;
import com.healthlink.domain.record.dto.DrugInteractionRequest;
import com.healthlink.domain.record.dto.DrugInteractionResponse;
import com.healthlink.domain.record.service.PrescriptionService;
import com.healthlink.domain.record.service.DrugInteractionService;
import com.healthlink.security.annotation.PhiAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Prescription management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final DrugInteractionService drugInteractionService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Create a prescription with automatic interaction check")
    @ApiResponse(responseCode = "200", description = "Prescription created")
    public PrescriptionResponse create(@Valid @RequestBody PrescriptionRequest request) {
        return prescriptionService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @PhiAccess(reason = "prescription_view", entityType = PrescriptionResponse.class, idParam = "id")
    @Operation(summary = "Get prescription by id")
    @ApiResponse(responseCode = "200", description = "Prescription found")
    public PrescriptionResponse get(@PathVariable UUID id) {
        return prescriptionService.get(id);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @PhiAccess(reason = "prescription_list_patient", entityType = PrescriptionResponse.class, idParam = "patientId")
    @Operation(summary = "List prescriptions for patient")
    @ApiResponse(responseCode = "200", description = "List of prescriptions")
    public List<PrescriptionResponse> listForPatient(@PathVariable UUID patientId) {
        return prescriptionService.listForPatient(patientId);
    }

    @PostMapping("/interactions")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Check drug interaction warnings without creating a prescription")
    @ApiResponse(responseCode = "200", description = "Interaction check complete")
    public ResponseEntity<DrugInteractionResponse> checkInteractions(@RequestBody DrugInteractionRequest request) {
        return ResponseEntity.ok(drugInteractionService.checkInteractions(request));
    }
}
