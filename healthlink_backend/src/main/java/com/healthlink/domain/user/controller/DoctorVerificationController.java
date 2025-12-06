package com.healthlink.domain.user.controller;

import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.repository.DoctorRepository;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.dto.ResponseEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for admin/staff to verify doctor PMDC licenses
 * This is separate from account approval - PMDC verification can happen before or after approval
 */
@RestController
@RequestMapping("/api/v1/admin/doctors")
@Tag(name = "Doctor Verification", description = "Admin/Staff endpoints for verifying doctor PMDC licenses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class DoctorVerificationController {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    /**
     * Get all doctors with their PMDC verification status
     */
    @GetMapping("/verification-status")
    @Operation(summary = "Get all doctors with PMDC verification status")
    @ApiResponse(responseCode = "200", description = "List of doctors with verification status")
    public ResponseEntity<ResponseEnvelope<List<DoctorVerificationStatusDto>>> getVerificationStatus() {
        List<Doctor> doctors = doctorRepository.findAll();
        
        List<DoctorVerificationStatusDto> statusList = doctors.stream()
                .map(doctor -> DoctorVerificationStatusDto.builder()
                        .doctorId(doctor.getId().toString())
                        .firstName(doctor.getFirstName())
                        .lastName(doctor.getLastName())
                        .email(doctor.getEmail())
                        .pmdcId(doctor.getPmdcId())
                        .pmdcVerified(doctor.getPmdcVerified() != null && doctor.getPmdcVerified())
                        .approvalStatus(doctor.getApprovalStatus().name())
                        .licenseDocumentUrl(doctor.getLicenseDocumentUrl())
                        .specialization(doctor.getSpecialization())
                        .build())
                .collect(Collectors.toList());

        ResponseEnvelope<List<DoctorVerificationStatusDto>> response = ResponseEnvelope.<List<DoctorVerificationStatusDto>>builder()
                .data(statusList)
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId("doctor-verification-status")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get doctors pending PMDC verification
     */
    @GetMapping("/pending-verification")
    @Operation(summary = "Get doctors pending PMDC verification")
    @ApiResponse(responseCode = "200", description = "List of doctors pending verification")
    public ResponseEntity<ResponseEnvelope<List<DoctorVerificationStatusDto>>> getPendingVerification() {
        List<Doctor> doctors = doctorRepository.findAll().stream()
                .filter(doctor -> doctor.getPmdcVerified() == null || !doctor.getPmdcVerified())
                .collect(Collectors.toList());

        List<DoctorVerificationStatusDto> statusList = doctors.stream()
                .map(doctor -> DoctorVerificationStatusDto.builder()
                        .doctorId(doctor.getId().toString())
                        .firstName(doctor.getFirstName())
                        .lastName(doctor.getLastName())
                        .email(doctor.getEmail())
                        .pmdcId(doctor.getPmdcId())
                        .pmdcVerified(false)
                        .approvalStatus(doctor.getApprovalStatus().name())
                        .licenseDocumentUrl(doctor.getLicenseDocumentUrl())
                        .specialization(doctor.getSpecialization())
                        .build())
                .collect(Collectors.toList());

        ResponseEnvelope<List<DoctorVerificationStatusDto>> response = ResponseEnvelope.<List<DoctorVerificationStatusDto>>builder()
                .data(statusList)
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId("pending-verification")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Verify a doctor's PMDC license
     * Admin/Staff reviews the PMDC ID and license document, then marks it as verified
     */
    @PostMapping("/{doctorId}/verify-pmdc")
    @Operation(summary = "Verify a doctor's PMDC license", 
               description = "Admin/Staff verifies the doctor's PMDC license after reviewing their license document. " +
                           "This is separate from account approval - a doctor can be PMDC verified but not yet approved, " +
                           "or approved but not yet PMDC verified.")
    @ApiResponse(responseCode = "200", description = "PMDC license verified")
    public ResponseEntity<ResponseEnvelope<DoctorVerificationStatusDto>> verifyPmdc(
            @PathVariable UUID doctorId,
            @RequestParam(required = false) String notes) {
        
        User user = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + doctorId));

        if (!(user instanceof Doctor)) {
            throw new RuntimeException("User is not a doctor");
        }

        Doctor doctor = (Doctor) user;

        doctor.setPmdcVerified(true);
        doctorRepository.save(doctor);

        DoctorVerificationStatusDto status = DoctorVerificationStatusDto.builder()
                .doctorId(doctor.getId().toString())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .email(doctor.getEmail())
                .pmdcId(doctor.getPmdcId())
                .pmdcVerified(true)
                .approvalStatus(doctor.getApprovalStatus().name())
                .licenseDocumentUrl(doctor.getLicenseDocumentUrl())
                .specialization(doctor.getSpecialization())
                .build();

        ResponseEnvelope<DoctorVerificationStatusDto> response = ResponseEnvelope.<DoctorVerificationStatusDto>builder()
                .data(status)
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId("pmdc-verified")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Revoke PMDC verification (if license is found to be invalid)
     */
    @PostMapping("/{doctorId}/revoke-pmdc")
    @Operation(summary = "Revoke PMDC verification", 
               description = "Revoke PMDC verification if the license is found to be invalid or fraudulent")
    @ApiResponse(responseCode = "200", description = "PMDC verification revoked")
    public ResponseEntity<ResponseEnvelope<DoctorVerificationStatusDto>> revokePmdc(
            @PathVariable UUID doctorId,
            @RequestParam(required = false) String reason) {
        
        User user = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + doctorId));

        if (!(user instanceof Doctor)) {
            throw new RuntimeException("User is not a doctor");
        }

        Doctor doctor = (Doctor) user;

        doctor.setPmdcVerified(false);
        doctorRepository.save(doctor);

        DoctorVerificationStatusDto status = DoctorVerificationStatusDto.builder()
                .doctorId(doctor.getId().toString())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .email(doctor.getEmail())
                .pmdcId(doctor.getPmdcId())
                .pmdcVerified(false)
                .approvalStatus(doctor.getApprovalStatus().name())
                .licenseDocumentUrl(doctor.getLicenseDocumentUrl())
                .specialization(doctor.getSpecialization())
                .build();

        ResponseEnvelope<DoctorVerificationStatusDto> response = ResponseEnvelope.<DoctorVerificationStatusDto>builder()
                .data(status)
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId("pmdc-revoked")
                .build();

        return ResponseEntity.ok(response);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DoctorVerificationStatusDto {
        private String doctorId;
        private String firstName;
        private String lastName;
        private String email;
        private String pmdcId;
        private Boolean pmdcVerified;
        private String approvalStatus;
        private String licenseDocumentUrl;
        private String specialization;
    }
}

