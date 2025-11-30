package com.healthlink.domain.user.controller;

import com.healthlink.domain.user.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors", description = "Doctor management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/{doctorId}/dashboard")
    @PreAuthorize("hasRole('DOCTOR') and principal.id == #doctorId")
    @Operation(summary = "Get doctor dashboard analytics")
    @ApiResponse(responseCode = "200", description = "Dashboard data retrieved")
    public ResponseEntity<com.healthlink.domain.user.dto.DoctorDashboardDTO> getDashboard(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(doctorService.getDashboard(doctorId));
    }

    @GetMapping("/{doctorId}/refund-policy")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    @Operation(summary = "Get doctor refund policy")
    @ApiResponse(responseCode = "200", description = "Refund policy retrieved")
    public ResponseEntity<Map<String, Object>> getRefundPolicy(@PathVariable UUID doctorId) {
        com.healthlink.domain.user.entity.Doctor doctor = (com.healthlink.domain.user.entity.Doctor) doctorService
                .getDoctorById(doctorId);
        return ResponseEntity.ok(Map.of(
                "doctorId", doctorId,
                "cutoffMinutes", doctor.getRefundCutoffMinutes(),
                "deductionPercent", doctor.getRefundDeductionPercent(),
                "allowDoctorCancellationFullRefund", doctor.getAllowFullRefundOnDoctorCancellation()));
    }
}
