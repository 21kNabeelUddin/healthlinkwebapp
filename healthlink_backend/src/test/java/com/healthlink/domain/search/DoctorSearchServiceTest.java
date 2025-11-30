package com.healthlink.domain.search;

import com.healthlink.domain.search.document.DoctorDocument;
import com.healthlink.domain.search.repository.DoctorSearchRepository;
import com.healthlink.domain.search.service.DoctorSearchService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorSearchServiceTest {

    @Mock
    private DoctorSearchRepository repository;

    @InjectMocks
    private DoctorSearchService service;

    @Test
    void getDoctorProfileReturnsEnrichedResponse() {
        DoctorDocument.FacilitySummaryDocument facility = DoctorDocument.FacilitySummaryDocument.builder()
                .id("FAC-1")
                .name("Downtown Clinic")
                .address("123 Main Rd")
                .city("Karachi")
                .phoneNumber("+92-300-0000000")
                .latitude(24.8607)
                .longitude(67.0011)
                .build();

        DoctorDocument document = DoctorDocument.builder()
                .id("DOC-1")
                .name("Dr. Sana Khan")
                .photoUrl("https://cdn.healthlink/doctors/dkhan.png")
                .specialty("Cardiology")
                .qualifications("MBBS, FCPS")
                .bio("Heart specialist with 10 years experience")
                .experienceYears(10)
                .averageRating(4.7)
                .totalReviews(145)
                .isAvailable(true)
                .isAvailableForTelemedicine(true)
                .languages(List.of("English", "Urdu"))
                .services(List.of("Consultation", "Second Opinion"))
                .minConsultationFee(1500d)
                .maxConsultationFee(3000d)
                .feeCurrency("PKR")
                .facilities(List.of(facility))
                .consultationFee(BigDecimal.valueOf(2000))
                .build();

        when(repository.findById("DOC-1")).thenReturn(Optional.of(document));

        var profile = service.getDoctorProfile("DOC-1");

        assertEquals("Dr. Sana Khan", profile.getName());
        assertEquals("Cardiology", profile.getSpecialty());
        assertEquals(2, profile.getServices().size());
        assertEquals(1, profile.getFacilities().size());
        assertEquals(1500d, profile.getPriceRange().getMin());
        assertEquals(3000d, profile.getPriceRange().getMax());
        assertEquals("PKR", profile.getPriceRange().getCurrency());
    }

    @Test
    void getDoctorProfileThrowsWhenNotFound() {
        when(repository.findById("missing"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.getDoctorProfile("missing"));
    }
}
