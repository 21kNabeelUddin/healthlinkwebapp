package com.healthlink.domain.search.service;

import com.healthlink.domain.search.document.DoctorDocument;
import com.healthlink.domain.search.dto.DoctorProfileResponse;
import com.healthlink.domain.search.dto.DoctorSearchRequest;
import com.healthlink.domain.search.dto.DoctorSearchResponse;
import com.healthlink.domain.search.repository.DoctorSearchRepository;
import com.healthlink.domain.organization.entity.Facility;
import com.healthlink.domain.organization.entity.ServiceOffering;
import com.healthlink.domain.organization.repository.FacilityRepository;
import com.healthlink.domain.organization.repository.ServiceOfferingRepository;
import com.healthlink.domain.user.entity.Doctor;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.data.elasticsearch.repositories", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DoctorSearchService {

    private final DoctorSearchRepository searchRepository;
    private final FacilityRepository facilityRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;

    /**
     * Search doctors with multiple filters
     * Uses repository query methods for simple filtering
     */
    public List<DoctorSearchResponse> searchDoctors(DoctorSearchRequest request) {
        List<DoctorDocument> results = new ArrayList<>();

        // Determine sort order
        Sort sort = determineSorting(request.getSortBy());

        // Apply filters using repository methods
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            results = searchRepository.findByNameContainingOrSpecialtyContaining(request.getQuery(), request.getQuery());
        } else if (request.getSpecialty() != null && request.getCity() != null) {
            results = searchRepository.findBySpecialtyAndCityOrderByAverageRatingDesc(
                    request.getSpecialty(), request.getCity()
            );
        } else if (request.getSpecialty() != null) {
            results = searchRepository.findBySpecialtyOrderByAverageRatingDesc(request.getSpecialty());
        } else if (request.getCity() != null) {
            results = searchRepository.findByCityOrderByAverageRatingDesc(request.getCity());
        } else if (request.getMinRating() != null) {
            results = searchRepository.findByAverageRatingGreaterThanEqualOrderByAverageRatingDesc(
                    request.getMinRating()
            );
        } else if (Boolean.TRUE.equals(request.getAvailableOnly())) {
            results = searchRepository.findByIsAvailableTrueOrderByAverageRatingDesc();
        } else {
            // Return all doctors sorted by rating
            results = StreamSupport.stream(searchRepository.findAll(sort).spliterator(), false)
                    .collect(Collectors.toList());
        }

        // Post-filter for additional criteria
        if (request.getArea() != null) {
            String area = request.getArea().toLowerCase();
            results = results.stream()
                    .filter(doc -> doc.getArea() != null && doc.getArea().toLowerCase().contains(area))
                    .collect(Collectors.toList());
        }

        if (request.getMinRating() != null && request.getSpecialty() == null) {
            double minRating = request.getMinRating();
            results = results.stream()
                    .filter(doc -> doc.getAverageRating() != null && doc.getAverageRating() >= minRating)
                    .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(request.getAvailableOnly()) && request.getSpecialty() != null) {
            results = results.stream()
                    .filter(doc -> Boolean.TRUE.equals(doc.getIsAvailable()))
                    .collect(Collectors.toList());
        }

        return results.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Sort determineSorting(String sortBy) {
        if (sortBy == null) {
            sortBy = "rating";
        }
        return switch (sortBy) {
            case "experience" -> Sort.by(Sort.Direction.DESC, "experienceYears");
            case "fee" -> Sort.by(Sort.Direction.ASC, "consultationFee");
            default -> Sort.by(Sort.Direction.DESC, "averageRating");
        };
    }

    /**
     * Index a doctor document
     */
    public void indexDoctor(DoctorDocument document) {
        searchRepository.save(document);
    }

    /**
     * Delete a doctor from index
     */
    public void deleteDoctor(String doctorId) {
        searchRepository.deleteById(doctorId);
    }

    /**
     * Load a full doctor profile for detail views
     */
    public DoctorProfileResponse getDoctorProfile(String doctorId) {
        DoctorDocument document = searchRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found: " + doctorId));
        return toProfileResponse(document);
    }

    private DoctorSearchResponse toResponse(DoctorDocument doc) {
        return DoctorSearchResponse.builder()
                .id(doc.getId())
                .name(doc.getName())
                .photoUrl(doc.getPhotoUrl())
                .specialty(doc.getSpecialty())
                .qualifications(doc.getQualifications())
                .experienceYears(doc.getExperienceYears())
                .city(doc.getCity())
                .area(doc.getArea())
                .averageRating(doc.getAverageRating())
                .totalReviews(doc.getTotalReviews())
                .consultationFee(doc.getConsultationFee())
                .facilityNames(safeList(doc.getFacilityNames()))
                .languages(safeList(doc.getLanguages()))
                .services(safeList(doc.getServices()))
                .isAvailable(doc.getIsAvailable())
                .isAvailableForTelemedicine(doc.getIsAvailableForTelemedicine())
                .organizationName(doc.getOrganizationName())
                .build();
    }

    private DoctorProfileResponse toProfileResponse(DoctorDocument doc) {
        return DoctorProfileResponse.builder()
                .id(doc.getId())
                .name(doc.getName())
                .photoUrl(doc.getPhotoUrl())
                .specialty(doc.getSpecialty())
                .qualification(doc.getQualifications())
                .yearsOfExperience(doc.getExperienceYears())
                .rating(Optional.ofNullable(doc.getAverageRating()).orElse(0d))
                .reviewCount(Optional.ofNullable(doc.getTotalReviews()).orElse(0))
                .bio(doc.getBio())
                .isAvailable(Boolean.TRUE.equals(doc.getIsAvailable()))
                .isAvailableForTelemedicine(Boolean.TRUE.equals(doc.getIsAvailableForTelemedicine()))
                .languages(safeList(doc.getLanguages()))
                .services(safeList(doc.getServices()))
                .facilities(safeList(doc.getFacilities()).stream()
                        .map(f -> DoctorProfileResponse.FacilitySummary.builder()
                                .id(f.getId())
                                .name(f.getName())
                                .address(f.getAddress())
                                .city(f.getCity())
                                .phoneNumber(f.getPhoneNumber())
                                .latitude(f.getLatitude())
                                .longitude(f.getLongitude())
                                .build())
                        .collect(Collectors.toList()))
                .priceRange(buildPriceRange(doc))
                .build();
    }

    private DoctorProfileResponse.PriceRange buildPriceRange(DoctorDocument doc) {
        Double min = doc.getMinConsultationFee();
        Double max = doc.getMaxConsultationFee();
        String currency = Optional.ofNullable(doc.getFeeCurrency()).orElse("PKR");

        if (min == null) {
            min = toDouble(doc.getConsultationFee());
        }
        if (max == null) {
            max = toDouble(doc.getConsultationFee());
        }

        return DoctorProfileResponse.PriceRange.builder()
                .min(min)
                .max(max)
                .currency(currency)
                .build();
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0d;
    }

    private <T> List<T> safeList(List<T> source) {
        return source != null ? source : Collections.emptyList();
    }

    /**
     * Index a doctor entity
     */
    public void indexDoctor(Doctor doctor) {
        List<Facility> facilities = facilityRepository.findByDoctorOwnerId(doctor.getId());
        List<String> facilityNames = facilities.stream().map(Facility::getName).collect(Collectors.toList());
        
        List<String> services = new ArrayList<>();
        List<DoctorDocument.FacilitySummaryDocument> facilityDocs = new ArrayList<>();

        for (Facility f : facilities) {
            List<ServiceOffering> offerings = serviceOfferingRepository.findByFacilityId(f.getId());
            services.addAll(offerings.stream().map(ServiceOffering::getName).collect(Collectors.toList()));
            
            facilityDocs.add(DoctorDocument.FacilitySummaryDocument.builder()
                    .id(f.getId().toString())
                    .name(f.getName())
                    .address(f.getAddress())
                    // City/Phone not in Facility entity directly? Check Facility.java
                    // Facility has address, but maybe not city/phone separate?
                    // Let's check Facility.java again. It has address.
                    .build());
        }

        DoctorDocument doc = DoctorDocument.builder()
                .id(doctor.getId().toString())
                .name(doctor.getFullName())
                .photoUrl(doctor.getProfilePictureUrl())
                .email(doctor.getEmail())
                .specialty(doctor.getSpecialization())
                .qualifications(String.join(", ", doctor.getQualifications()))
                .bio(doctor.getBio())
                .experienceYears(doctor.getYearsOfExperience())
                // City/Area not in Doctor entity? 
                // Maybe derive from first facility?
                .averageRating(doctor.getAverageRating())
                .totalReviews(doctor.getTotalReviews())
                .consultationFee(doctor.getConsultationFee())
                .facilityNames(facilityNames)
                .services(services.stream().distinct().collect(Collectors.toList()))
                .isAvailable(true) // Default to true for now, logic needed for schedule
                .isAvailableForTelemedicine(true) // Default
                .facilities(facilityDocs)
                .build();

        searchRepository.save(doc);
    }
}
