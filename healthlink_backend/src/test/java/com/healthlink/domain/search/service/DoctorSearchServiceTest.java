package com.healthlink.domain.search.service;

import com.healthlink.domain.search.document.DoctorDocument;
import com.healthlink.domain.search.dto.DoctorProfileResponse;
import com.healthlink.domain.search.dto.DoctorSearchRequest;
import com.healthlink.domain.search.dto.DoctorSearchResponse;
import com.healthlink.domain.search.repository.DoctorSearchRepository;
import com.healthlink.domain.organization.repository.FacilityRepository;
import com.healthlink.domain.organization.repository.ServiceOfferingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorSearchService Tests")
class DoctorSearchServiceTest {

    @Mock
    private DoctorSearchRepository searchRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private ServiceOfferingRepository serviceOfferingRepository;

    @InjectMocks
    private DoctorSearchService doctorSearchService;

    private DoctorDocument createDoctorDocument(String id, String name, String specialty, String city) {
        return DoctorDocument.builder()
                .id(id)
                .name(name)
                .specialty(specialty)
                .city(city)
                .qualifications("MBBS, MD")
                .experienceYears(10)
                .averageRating(4.5)
                .totalReviews(50)
                .consultationFee(BigDecimal.valueOf(2000))
                .isAvailable(true)
                .isAvailableForTelemedicine(true)
                .facilityNames(List.of("City Hospital"))
                .languages(List.of("English", "Urdu"))
                .services(List.of("Consultation", "Follow-up"))
                .build();
    }

    @Nested
    @DisplayName("Search Doctors Tests")
    class SearchDoctorsTests {

        @Test
        @DisplayName("Should search doctors by query string")
        void shouldSearchDoctorsByQuery() {
            // Arrange
            DoctorSearchRequest request = new DoctorSearchRequest();
            request.setQuery("cardio");
            
            List<DoctorDocument> expected = List.of(
                    createDoctorDocument("1", "Dr. Ali Khan", "Cardiology", "Karachi"),
                    createDoctorDocument("2", "Dr. Sara Ahmed", "Cardiology", "Lahore")
            );
            
            when(searchRepository.findByNameContainingOrSpecialtyContaining("cardio", "cardio"))
                    .thenReturn(expected);

            // Act
            List<DoctorSearchResponse> results = doctorSearchService.searchDoctors(request);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getName()).isEqualTo("Dr. Ali Khan");
            verify(searchRepository).findByNameContainingOrSpecialtyContaining("cardio", "cardio");
        }

        @Test
        @DisplayName("Should search doctors by specialty")
        void shouldSearchDoctorsBySpecialty() {
            // Arrange
            DoctorSearchRequest request = new DoctorSearchRequest();
            request.setSpecialty("Dermatology");
            
            List<DoctorDocument> expected = List.of(
                    createDoctorDocument("3", "Dr. Fatima Zahra", "Dermatology", "Islamabad")
            );
            
            when(searchRepository.findBySpecialtyOrderByAverageRatingDesc("Dermatology"))
                    .thenReturn(expected);

            // Act
            List<DoctorSearchResponse> results = doctorSearchService.searchDoctors(request);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getSpecialty()).isEqualTo("Dermatology");
        }

        @Test
        @DisplayName("Should search doctors by city")
        void shouldSearchDoctorsByCity() {
            // Arrange
            DoctorSearchRequest request = new DoctorSearchRequest();
            request.setCity("Lahore");
            
            List<DoctorDocument> expected = List.of(
                    createDoctorDocument("4", "Dr. Hassan", "General", "Lahore"),
                    createDoctorDocument("5", "Dr. Ayesha", "Pediatrics", "Lahore")
            );
            
            when(searchRepository.findByCityOrderByAverageRatingDesc("Lahore"))
                    .thenReturn(expected);

            // Act
            List<DoctorSearchResponse> results = doctorSearchService.searchDoctors(request);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(r -> "Lahore".equals(r.getCity()));
        }

        @Test
        @DisplayName("Should search doctors by specialty and city combined")
        void shouldSearchDoctorsBySpecialtyAndCity() {
            // Arrange
            DoctorSearchRequest request = new DoctorSearchRequest();
            request.setSpecialty("Cardiology");
            request.setCity("Karachi");
            
            List<DoctorDocument> expected = List.of(
                    createDoctorDocument("6", "Dr. Imran", "Cardiology", "Karachi")
            );
            
            when(searchRepository.findBySpecialtyAndCityOrderByAverageRatingDesc("Cardiology", "Karachi"))
                    .thenReturn(expected);

            // Act
            List<DoctorSearchResponse> results = doctorSearchService.searchDoctors(request);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getCity()).isEqualTo("Karachi");
            assertThat(results.get(0).getSpecialty()).isEqualTo("Cardiology");
        }

        @Test
        @DisplayName("Should filter doctors by minimum rating")
        void shouldFilterDoctorsByMinRating() {
            // Arrange
            DoctorSearchRequest request = new DoctorSearchRequest();
            request.setMinRating(4.0);
            
            DoctorDocument doc = createDoctorDocument("7", "Dr. Top Rated", "Neurology", "Karachi");
            doc.setAverageRating(4.8);
            List<DoctorDocument> expected = List.of(doc);
            
            when(searchRepository.findByAverageRatingGreaterThanEqualOrderByAverageRatingDesc(4.0))
                    .thenReturn(expected);

            // Act
            List<DoctorSearchResponse> results = doctorSearchService.searchDoctors(request);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAverageRating()).isGreaterThanOrEqualTo(4.0);
        }

        @Test
        @DisplayName("Should filter only available doctors")
        void shouldFilterAvailableDoctors() {
            // Arrange
            DoctorSearchRequest request = new DoctorSearchRequest();
            request.setAvailableOnly(true);
            
            List<DoctorDocument> expected = List.of(
                    createDoctorDocument("8", "Dr. Available", "General", "Multan")
            );
            
            when(searchRepository.findByIsAvailableTrueOrderByAverageRatingDesc())
                    .thenReturn(expected);

            // Act
            List<DoctorSearchResponse> results = doctorSearchService.searchDoctors(request);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getIsAvailable()).isTrue();
        }

        @Test
        @DisplayName("Should return empty list when no doctors match")
        void shouldReturnEmptyListWhenNoMatch() {
            // Arrange
            DoctorSearchRequest request = new DoctorSearchRequest();
            request.setSpecialty("RareSpecialty");
            
            when(searchRepository.findBySpecialtyOrderByAverageRatingDesc("RareSpecialty"))
                    .thenReturn(Collections.emptyList());

            // Act
            List<DoctorSearchResponse> results = doctorSearchService.searchDoctors(request);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should filter by area post-query")
        void shouldFilterByArea() {
            // Arrange
            DoctorSearchRequest request = new DoctorSearchRequest();
            request.setCity("Karachi");
            request.setArea("Clifton");
            
            DoctorDocument doc1 = createDoctorDocument("9", "Dr. Clifton", "General", "Karachi");
            doc1.setArea("Clifton");
            DoctorDocument doc2 = createDoctorDocument("10", "Dr. DHA", "General", "Karachi");
            doc2.setArea("DHA");
            
            when(searchRepository.findByCityOrderByAverageRatingDesc("Karachi"))
                    .thenReturn(List.of(doc1, doc2));

            // Act
            List<DoctorSearchResponse> results = doctorSearchService.searchDoctors(request);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getArea()).containsIgnoringCase("Clifton");
        }
    }

    @Nested
    @DisplayName("Get Doctor Profile Tests")
    class GetDoctorProfileTests {

        @Test
        @DisplayName("Should get doctor profile by ID")
        void shouldGetDoctorProfileById() {
            // Arrange
            String doctorId = "doc-123";
            DoctorDocument doc = createDoctorDocument(doctorId, "Dr. Profile Test", "Cardiology", "Karachi");
            doc.setBio("Experienced cardiologist with 15 years of practice");
            doc.setMinConsultationFee(1500.0);
            doc.setMaxConsultationFee(3000.0);
            doc.setFeeCurrency("PKR");
            doc.setFacilities(List.of(
                    DoctorDocument.FacilitySummaryDocument.builder()
                            .id("fac-1")
                            .name("City Hospital")
                            .address("123 Main Street")
                            .city("Karachi")
                            .build()
            ));
            
            when(searchRepository.findById(doctorId)).thenReturn(Optional.of(doc));

            // Act
            DoctorProfileResponse profile = doctorSearchService.getDoctorProfile(doctorId);

            // Assert
            assertThat(profile.getId()).isEqualTo(doctorId);
            assertThat(profile.getName()).isEqualTo("Dr. Profile Test");
            assertThat(profile.getSpecialty()).isEqualTo("Cardiology");
            assertThat(profile.getBio()).contains("cardiologist");
            assertThat(profile.getFacilities()).hasSize(1);
            assertThat(profile.getPriceRange().getMin()).isEqualTo(1500.0);
            assertThat(profile.getPriceRange().getMax()).isEqualTo(3000.0);
            assertThat(profile.getPriceRange().getCurrency()).isEqualTo("PKR");
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when doctor not found")
        void shouldThrowExceptionWhenDoctorNotFound() {
            // Arrange
            String doctorId = "non-existent";
            when(searchRepository.findById(doctorId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> doctorSearchService.getDoctorProfile(doctorId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Doctor not found");
        }

        @Test
        @DisplayName("Should handle profile with missing price range")
        void shouldHandleMissingPriceRange() {
            // Arrange
            String doctorId = "doc-124";
            DoctorDocument doc = createDoctorDocument(doctorId, "Dr. No Price", "General", "Lahore");
            doc.setMinConsultationFee(null);
            doc.setMaxConsultationFee(null);
            doc.setConsultationFee(BigDecimal.valueOf(2000));
            
            when(searchRepository.findById(doctorId)).thenReturn(Optional.of(doc));

            // Act
            DoctorProfileResponse profile = doctorSearchService.getDoctorProfile(doctorId);

            // Assert
            assertThat(profile.getPriceRange()).isNotNull();
            assertThat(profile.getPriceRange().getMin()).isEqualTo(2000.0);
            assertThat(profile.getPriceRange().getMax()).isEqualTo(2000.0);
        }

        @Test
        @DisplayName("Should return profile with empty facilities list")
        void shouldHandleEmptyFacilities() {
            // Arrange
            String doctorId = "doc-125";
            DoctorDocument doc = createDoctorDocument(doctorId, "Dr. No Facility", "General", "Karachi");
            doc.setFacilities(null);
            
            when(searchRepository.findById(doctorId)).thenReturn(Optional.of(doc));

            // Act
            DoctorProfileResponse profile = doctorSearchService.getDoctorProfile(doctorId);

            // Assert
            assertThat(profile.getFacilities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Index and Delete Doctor Tests")
    class IndexDeleteTests {

        @Test
        @DisplayName("Should index doctor document")
        void shouldIndexDoctorDocument() {
            // Arrange
            DoctorDocument doc = createDoctorDocument("new-doc", "Dr. New", "Pediatrics", "Quetta");
            
            when(searchRepository.save(any(DoctorDocument.class))).thenReturn(doc);

            // Act
            doctorSearchService.indexDoctor(doc);

            // Assert
            verify(searchRepository).save(doc);
        }

        @Test
        @DisplayName("Should delete doctor from index")
        void shouldDeleteDoctorFromIndex() {
            // Arrange
            String doctorId = "doc-to-delete";

            // Act
            doctorSearchService.deleteDoctor(doctorId);

            // Assert
            verify(searchRepository).deleteById(doctorId);
        }
    }

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should map all fields to response")
        void shouldMapAllFieldsToResponse() {
            // Arrange
            DoctorSearchRequest request = new DoctorSearchRequest();
            request.setSpecialty("Cardiology");
            
            DoctorDocument doc = DoctorDocument.builder()
                    .id("map-test")
                    .name("Dr. Complete")
                    .photoUrl("https://example.com/photo.jpg")
                    .specialty("Cardiology")
                    .qualifications("MBBS, FRCP")
                    .experienceYears(20)
                    .city("Islamabad")
                    .area("F-8")
                    .averageRating(4.9)
                    .totalReviews(200)
                    .consultationFee(BigDecimal.valueOf(5000))
                    .facilityNames(List.of("PIMS", "Shifa"))
                    .languages(List.of("English", "Urdu", "Punjabi"))
                    .services(List.of("ECG", "Echo", "Stress Test"))
                    .isAvailable(true)
                    .isAvailableForTelemedicine(true)
                    .organizationName("Medical Associates")
                    .build();
            
            when(searchRepository.findBySpecialtyOrderByAverageRatingDesc("Cardiology"))
                    .thenReturn(List.of(doc));

            // Act
            List<DoctorSearchResponse> results = doctorSearchService.searchDoctors(request);

            // Assert
            DoctorSearchResponse response = results.get(0);
            assertThat(response.getId()).isEqualTo("map-test");
            assertThat(response.getName()).isEqualTo("Dr. Complete");
            assertThat(response.getPhotoUrl()).isEqualTo("https://example.com/photo.jpg");
            assertThat(response.getSpecialty()).isEqualTo("Cardiology");
            assertThat(response.getQualifications()).isEqualTo("MBBS, FRCP");
            assertThat(response.getExperienceYears()).isEqualTo(20);
            assertThat(response.getCity()).isEqualTo("Islamabad");
            assertThat(response.getArea()).isEqualTo("F-8");
            assertThat(response.getAverageRating()).isEqualTo(4.9);
            assertThat(response.getTotalReviews()).isEqualTo(200);
            assertThat(response.getConsultationFee()).isEqualByComparingTo(BigDecimal.valueOf(5000));
            assertThat(response.getFacilityNames()).containsExactly("PIMS", "Shifa");
            assertThat(response.getLanguages()).containsExactly("English", "Urdu", "Punjabi");
            assertThat(response.getServices()).containsExactly("ECG", "Echo", "Stress Test");
            assertThat(response.getIsAvailable()).isTrue();
            assertThat(response.getIsAvailableForTelemedicine()).isTrue();
            assertThat(response.getOrganizationName()).isEqualTo("Medical Associates");
        }

        @Test
        @DisplayName("Should handle null lists gracefully")
        void shouldHandleNullListsGracefully() {
            // Arrange
            DoctorSearchRequest request = new DoctorSearchRequest();
            request.setSpecialty("Nephrology");
            
            DoctorDocument doc = DoctorDocument.builder()
                    .id("null-test")
                    .name("Dr. Minimal")
                    .specialty("Nephrology")
                    .build();
            // Facility names, languages, services are null
            
            when(searchRepository.findBySpecialtyOrderByAverageRatingDesc("Nephrology"))
                    .thenReturn(List.of(doc));

            // Act
            List<DoctorSearchResponse> results = doctorSearchService.searchDoctors(request);

            // Assert
            DoctorSearchResponse response = results.get(0);
            assertThat(response.getFacilityNames()).isEmpty();
            assertThat(response.getLanguages()).isEmpty();
            assertThat(response.getServices()).isEmpty();
        }
    }
}
