package com.healthlink.domain.record.controller;

import com.healthlink.domain.record.dto.MedicalRecordResponse;
import com.healthlink.domain.record.entity.RecordType;
import com.healthlink.domain.record.service.MedicalRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Encounters", description = "Encounter management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class EncounterController {

    private final MedicalRecordService medicalRecordService;
    private final com.healthlink.domain.appointment.repository.AppointmentRepository appointmentRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/{appointmentId}/encounters")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Save an encounter")
    public ResponseEntity<MedicalRecordResponse> saveEncounter(
            @PathVariable UUID appointmentId,
            @RequestBody Map<String, Object> encounterData) {
        try {
            var appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new RuntimeException("Appointment not found"));
            
            UUID patientId = appointment.getPatient().getId();
            UUID doctorId = appointment.getDoctor().getId();

            String jsonContent = objectMapper.writeValueAsString(encounterData);

                return ResponseEntity.ok(medicalRecordService.createStructuredRecord(
                    patientId, doctorId, RecordType.HISTORY.name(), jsonContent));
        } catch (Exception e) {
             throw new RuntimeException("Error saving encounter: " + e.getMessage());
        }
    }
}
