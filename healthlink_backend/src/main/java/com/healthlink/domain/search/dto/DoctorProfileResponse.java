package com.healthlink.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Rich doctor profile payload consumed by mobile apps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfileResponse {
    private String id;
    private String name;
    private String photoUrl;
    private String specialty;
    private String qualification;
    private Integer yearsOfExperience;
    private Double rating;
    private Integer reviewCount;
    private String bio;
    private Boolean isAvailable;
    private Boolean isAvailableForTelemedicine;
    private List<String> languages;
    private List<String> services;
    private List<FacilitySummary> facilities;
    private PriceRange priceRange;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacilitySummary {
        private String id;
        private String name;
        private String address;
        private String city;
        private String phoneNumber;
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceRange {
        private Double min;
        private Double max;
        private String currency;
    }
}
