package com.healthlink.controller;

import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.repository.DoctorRepository;
import com.healthlink.dto.ResponseEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple doctor listing endpoint that works without Elasticsearch
 * Provides fallback when search functionality is not available
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DoctorListController {

    private final DoctorRepository doctorRepository;

    /**
     * List all verified and approved doctors
     * Optional specialization filter
     */
    @GetMapping("/doctors")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<ResponseEnvelope<List<DoctorListItem>>> listDoctors(
            @RequestParam(required = false) String specialization) {
        
        List<Doctor> doctors;
        if (specialization != null && !specialization.trim().isEmpty()) {
            doctors = doctorRepository.searchBySpecialization(specialization.trim());
        } else {
            doctors = doctorRepository.findAllVerifiedAndApproved();
        }

        List<DoctorListItem> doctorList = doctors.stream()
                .map(doctor -> DoctorListItem.builder()
                        .id(doctor.getId().toString())
                        .firstName(doctor.getFirstName())
                        .lastName(doctor.getLastName())
                        .email(doctor.getEmail())
                        .phoneNumber(doctor.getPhoneNumber())
                        .specialization(doctor.getSpecialization())
                        .licenseNumber(doctor.getPmdcId())
                        .yearsOfExperience(doctor.getYearsOfExperience())
                        .averageRating(doctor.getAverageRating())
                        .totalReviews(doctor.getTotalReviews())
                        .consultationFee(doctor.getConsultationFee())
                        .build())
                .collect(Collectors.toList());

        ResponseEnvelope<List<DoctorListItem>> response = ResponseEnvelope.<List<DoctorListItem>>builder()
                .data(doctorList)
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId("doctor-list")
                .build();

        return ResponseEntity.ok(response);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DoctorListItem {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String specialization;
        private String licenseNumber;
        private Integer yearsOfExperience;
        private Double averageRating;
        private Integer totalReviews;
        private java.math.BigDecimal consultationFee;
    }
}

