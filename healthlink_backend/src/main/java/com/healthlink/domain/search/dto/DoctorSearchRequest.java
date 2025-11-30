package com.healthlink.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSearchRequest {
    private String query; // General search query (name, specialty, etc.)
    private String specialty;
    private String city;
    private String area;
    private Double minRating;
    private Boolean availableOnly;
    private String sortBy; // rating, experience, fee
}
