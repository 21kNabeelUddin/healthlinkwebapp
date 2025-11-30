package com.healthlink.domain.record.controller;

import com.healthlink.domain.record.dto.FamilyMedicalTreeResponse;
import com.healthlink.domain.record.dto.FamilyMedicalTreeRequest;
import com.healthlink.domain.record.service.FamilyMedicalTreeService;
import com.healthlink.security.annotation.PhiAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/family-history")
@Tag(name = "Family Medical Tree", description = "Patient hereditary condition tracking")
@RequiredArgsConstructor
public class FamilyMedicalTreeController {

    private final FamilyMedicalTreeService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR','STAFF') or (hasRole('PATIENT') and principal.id == #patientId)")
    @Operation(summary = "List patient hereditary conditions")
    @PhiAccess(reason = "family_history_view", entityType = FamilyMedicalTreeResponse.class, idParam = "patientId")
    public ResponseEntity<List<FamilyMedicalTreeResponse>> getFamilyHistory(@PathVariable UUID patientId) {
        return ResponseEntity.ok(service.forPatient(patientId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR') and (hasRole('PATIENT') ? principal.id == #patientId : true)")
    @Operation(summary = "Add family medical history entry")
    @PhiAccess(reason = "family_history_add", entityType = FamilyMedicalTreeResponse.class, idParam = "patientId")
    public ResponseEntity<FamilyMedicalTreeResponse> addFamilyMember(
            @PathVariable UUID patientId,
            @Valid @RequestBody FamilyMedicalTreeRequest request) {
        FamilyMedicalTreeResponse response = service.addEntry(patientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{memberId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR') and (hasRole('PATIENT') ? principal.id == #patientId : true)")
    @Operation(summary = "Update family medical history entry")
    @PhiAccess(reason = "family_history_update", entityType = FamilyMedicalTreeResponse.class, idParam = "patientId")
    public ResponseEntity<FamilyMedicalTreeResponse> updateFamilyMember(
            @PathVariable UUID patientId,
            @PathVariable UUID memberId,
            @Valid @RequestBody FamilyMedicalTreeRequest request) {
        FamilyMedicalTreeResponse response = service.updateEntry(memberId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{memberId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR') and (hasRole('PATIENT') ? principal.id == #patientId : true)")
    @Operation(summary = "Delete a family medical history node (patient only)")
    @PhiAccess(reason = "family_history_delete", entityType = FamilyMedicalTreeResponse.class, idParam = "patientId")
    public ResponseEntity<Void> deleteFamilyMember(
            @PathVariable UUID patientId,
            @PathVariable UUID memberId) {
        service.delete(memberId);
        return ResponseEntity.noContent().build();
    }
}
