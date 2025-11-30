package com.healthlink.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSearchResponse {
    private String id;
    private String name;
    private String specialty;
    private String qualifications;
    private Integer experienceYears;
    private String city;
    private String area;
    private Double averageRating;
    private Integer totalReviews;
    private BigDecimal consultationFee;
    private List<String> facilityNames;
    private List<String> languages;
    private List<String> services;
    private Boolean isAvailable;
    private Boolean isAvailableForTelemedicine;
    private String organizationName;
    private String photoUrl;
}
