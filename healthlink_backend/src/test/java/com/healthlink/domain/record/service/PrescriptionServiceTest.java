package com.healthlink.domain.record.service;

import com.healthlink.domain.record.dto.PrescriptionRequest;
import com.healthlink.domain.record.dto.PrescriptionResponse;
import com.healthlink.domain.record.entity.Prescription;
import com.healthlink.domain.record.entity.PrescriptionTemplate;
import com.healthlink.domain.record.repository.PrescriptionRepository;
import com.healthlink.domain.record.repository.PrescriptionTemplateRepository;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.security.model.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for PrescriptionService.
 * <p>
 * Tests cover:
 * - Prescription creation with drug interactions
 * - Template application
 * - RBAC enforcement (doctor-only creation)
 * - View permissions (doctor/patient access)
 * - Error handling
 * - Edge cases
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository repository;

    @Mock
    private PrescriptionTemplateRepository templateRepository;

    @Mock
    private OpenFdaDrugInteractionClient interactionClient;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private PrescriptionService prescriptionService;

    private UUID doctorId;
    private UUID patientId;
    private CustomUserDetails doctorDetails;
    private CustomUserDetails patientDetails;
    private Doctor doctorUser;
    private Patient patientUser;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();

        // Setup doctor user
        doctorUser = new Doctor();
        doctorUser.setId(doctorId);
        doctorUser.setEmail("doctor@healthlink.com");
        doctorUser.setPasswordHash("hashedPassword");
        doctorUser.setRole(UserRole.DOCTOR);
        doctorUser.setPmdcId("12345-P");
        doctorUser.setSpecialization("General Medicine");
        doctorDetails = new CustomUserDetails(doctorUser);

        // Setup patient user
        patientUser = new Patient();
        patientUser.setId(patientId);
        patientUser.setEmail("patient@healthlink.com");
        patientUser.setPasswordHash("hashedPassword");
        patientUser.setRole(UserRole.PATIENT);
        patientDetails = new CustomUserDetails(patientUser);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== PRESCRIPTION CREATION TESTS ==========

    @Nested
    @DisplayName("Prescription Creation Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create prescription successfully as doctor")
        void create_shouldCreatePrescriptionSuccessfully() {
            // Arrange
            PrescriptionRequest request = createValidRequest();

            when(authentication.getPrincipal()).thenReturn(doctorDetails);
            doReturn(doctorDetails.getAuthorities()).when(authentication).getAuthorities();
            when(interactionClient.fetchInteractions(anyString())).thenReturn(List.of());
            when(repository.save(any(Prescription.class))).thenAnswer(i -> {
                Prescription p = i.getArgument(0);
                if (p.getId() == null) {
                    p.setId(UUID.randomUUID());
                }
                p.setCreatedAt(LocalDateTime.now());
                return p;
            });

            // Act
            PrescriptionResponse response = prescriptionService.create(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("Antibiotic Treatment");
            assertThat(response.getMedications()).hasSize(2);
            assertThat(response.getDoctorId()).isEqualTo(doctorId);
            verify(repository).save(any(Prescription.class));
        }

        @Test
        @DisplayName("Should fetch and include drug interactions")
        void create_shouldFetchDrugInteractions() {
            // Arrange
            PrescriptionRequest request = createValidRequest();
            request.setMedications(Arrays.asList("Warfarin", "Aspirin"));

            when(authentication.getPrincipal()).thenReturn(doctorDetails);
            doReturn(doctorDetails.getAuthorities()).when(authentication).getAuthorities();
            when(interactionClient.fetchInteractions("warfarin"))
                    .thenReturn(List.of("Increased bleeding risk with NSAIDs"));
            when(interactionClient.fetchInteractions("aspirin"))
                    .thenReturn(List.of("Avoid with anticoagulants"));
            when(repository.save(any(Prescription.class))).thenAnswer(i -> {
                Prescription p = i.getArgument(0);
                if (p.getId() == null) p.setId(UUID.randomUUID());
                p.setCreatedAt(LocalDateTime.now());
                return p;
            });

            // Act
            PrescriptionResponse response = prescriptionService.create(request);

            // Assert
            assertThat(response.getInteractionWarnings()).hasSize(2);
            verify(interactionClient, times(2)).fetchInteractions(anyString());
        }

        @Test
        @DisplayName("Should deduplicate interaction warnings")
        void create_shouldDeduplicateInteractionWarnings() {
            // Arrange
            PrescriptionRequest request = createValidRequest();
            request.setMedications(Arrays.asList("DrugA", "DrugB"));
            String commonWarning = "Common interaction warning";

            when(authentication.getPrincipal()).thenReturn(doctorDetails);
            doReturn(doctorDetails.getAuthorities()).when(authentication).getAuthorities();
            when(interactionClient.fetchInteractions("druga")).thenReturn(List.of(commonWarning));
            when(interactionClient.fetchInteractions("drugb")).thenReturn(List.of(commonWarning));
            when(repository.save(any(Prescription.class))).thenAnswer(i -> {
                Prescription p = i.getArgument(0);
                if (p.getId() == null) p.setId(UUID.randomUUID());
                p.setCreatedAt(LocalDateTime.now());
                return p;
            });

            // Act
            PrescriptionResponse response = prescriptionService.create(request);

            // Assert
            assertThat(response.getInteractionWarnings()).hasSize(1);
            assertThat(response.getInteractionWarnings().get(0)).isEqualTo(commonWarning);
        }

        @Test
        @DisplayName("Should apply template when templateId is provided")
        void create_shouldApplyTemplate() {
            // Arrange
            UUID templateId = UUID.randomUUID();
            PrescriptionTemplate template = new PrescriptionTemplate();
            template.setId(templateId);
            template.setContent("Template: {{body}} - End of prescription.");

            PrescriptionRequest request = createValidRequest();
            request.setBody("Take medication as directed");
            request.setTemplateId(templateId);

            when(authentication.getPrincipal()).thenReturn(doctorDetails);
            doReturn(doctorDetails.getAuthorities()).when(authentication).getAuthorities();
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
            when(repository.save(any(Prescription.class))).thenAnswer(i -> {
                Prescription p = i.getArgument(0);
                if (p.getId() == null) p.setId(UUID.randomUUID());
                p.setCreatedAt(LocalDateTime.now());
                return p;
            });

            // Act
            PrescriptionResponse response = prescriptionService.create(request);

            // Assert
            assertThat(response.getBody())
                    .contains("Template:")
                    .contains("Take medication as directed")
                    .contains("End of prescription");
        }

        @Test
        @DisplayName("Should throw when template not found")
        void create_shouldThrowWhenTemplateNotFound() {
            // Arrange
            UUID templateId = UUID.randomUUID();
            PrescriptionRequest request = createValidRequest();
            request.setTemplateId(templateId);

            when(authentication.getPrincipal()).thenReturn(doctorDetails);
            doReturn(doctorDetails.getAuthorities()).when(authentication).getAuthorities();
            when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> prescriptionService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Template not found");
        }

        @Test
        @DisplayName("Should sanitize medications - lowercase and trim")
        void create_shouldSanitizeMedications() {
            // Arrange
            PrescriptionRequest request = createValidRequest();
            request.setMedications(Arrays.asList("  ASPIRIN  ", "Paracetamol  ", "  ibuprofen"));

            when(authentication.getPrincipal()).thenReturn(doctorDetails);
            doReturn(doctorDetails.getAuthorities()).when(authentication).getAuthorities();
            when(interactionClient.fetchInteractions(anyString())).thenReturn(List.of());
            when(repository.save(any(Prescription.class))).thenAnswer(i -> {
                Prescription p = i.getArgument(0);
                if (p.getId() == null) p.setId(UUID.randomUUID());
                p.setCreatedAt(LocalDateTime.now());
                return p;
            });

            // Act
            PrescriptionResponse response = prescriptionService.create(request);

            // Assert
            assertThat(response.getMedications())
                    .containsExactly("aspirin", "paracetamol", "ibuprofen");
        }

        @Test
        @DisplayName("Should handle empty medications list")
        void create_shouldHandleEmptyMedications() {
            // Arrange
            PrescriptionRequest request = createValidRequest();
            request.setMedications(Collections.emptyList());

            when(authentication.getPrincipal()).thenReturn(doctorDetails);
            doReturn(doctorDetails.getAuthorities()).when(authentication).getAuthorities();
            when(repository.save(any(Prescription.class))).thenAnswer(i -> {
                Prescription p = i.getArgument(0);
                if (p.getId() == null) p.setId(UUID.randomUUID());
                p.setCreatedAt(LocalDateTime.now());
                return p;
            });

            // Act
            PrescriptionResponse response = prescriptionService.create(request);

            // Assert
            assertThat(response.getMedications()).isEmpty();
            assertThat(response.getInteractionWarnings()).isEmpty();
            verify(interactionClient, never()).fetchInteractions(anyString());
        }

        @Test
        @DisplayName("Should handle null medications")
        void create_shouldHandleNullMedications() {
            // Arrange
            PrescriptionRequest request = createValidRequest();
            request.setMedications(null);

            when(authentication.getPrincipal()).thenReturn(doctorDetails);
            doReturn(doctorDetails.getAuthorities()).when(authentication).getAuthorities();
            when(repository.save(any(Prescription.class))).thenAnswer(i -> {
                Prescription p = i.getArgument(0);
                if (p.getId() == null) p.setId(UUID.randomUUID());
                p.setCreatedAt(LocalDateTime.now());
                return p;
            });

            // Act
            PrescriptionResponse response = prescriptionService.create(request);

            // Assert
            assertThat(response.getMedications()).isEmpty();
        }
    }

    // ========== RBAC TESTS ==========

    @Nested
    @DisplayName("RBAC Tests")
    class RbacTests {

        @Test
        @DisplayName("Should throw AccessDeniedException when non-doctor tries to create")
        void create_shouldThrowWhenNotDoctor() {
            // Arrange
            PrescriptionRequest request = createValidRequest();
            doReturn(patientDetails.getAuthorities()).when(authentication).getAuthorities();

            // Act & Assert
            assertThatThrownBy(() -> prescriptionService.create(request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Only doctors");
        }

        @Test
        @DisplayName("Should allow doctor to view any prescription")
        void get_shouldAllowDoctorToViewAnyPrescription() {
            // Arrange
            UUID prescriptionId = UUID.randomUUID();
            UUID otherPatientId = UUID.randomUUID();
            
            Prescription prescription = createTestPrescription(prescriptionId, otherPatientId);

            when(authentication.getPrincipal()).thenReturn(doctorDetails);
            doReturn(doctorDetails.getAuthorities()).when(authentication).getAuthorities();
            when(authentication.isAuthenticated()).thenReturn(true);
            when(repository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

            // Act
            PrescriptionResponse response = prescriptionService.get(prescriptionId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(prescriptionId);
        }

        @Test
        @DisplayName("Should allow patient to view their own prescription")
        void get_shouldAllowPatientToViewOwnPrescription() {
            // Arrange
            UUID prescriptionId = UUID.randomUUID();
            Prescription prescription = createTestPrescription(prescriptionId, patientId);

            when(authentication.getPrincipal()).thenReturn(patientDetails);
            doReturn(patientDetails.getAuthorities()).when(authentication).getAuthorities();
            when(authentication.isAuthenticated()).thenReturn(true);
            when(repository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

            // Act
            PrescriptionResponse response = prescriptionService.get(prescriptionId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo(patientId);
        }

        @Test
        @DisplayName("Should throw when patient tries to view another patient's prescription")
        void get_shouldThrowWhenPatientViewsOtherPrescription() {
            // Arrange
            UUID prescriptionId = UUID.randomUUID();
            UUID otherPatientId = UUID.randomUUID();
            Prescription prescription = createTestPrescription(prescriptionId, otherPatientId);

            when(authentication.getPrincipal()).thenReturn(patientDetails);
            doReturn(patientDetails.getAuthorities()).when(authentication).getAuthorities();
            when(authentication.isAuthenticated()).thenReturn(true);
            when(repository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

            // Act & Assert
            assertThatThrownBy(() -> prescriptionService.get(prescriptionId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Forbidden");
        }
    }

    // ========== LIST PRESCRIPTIONS TESTS ==========

    @Nested
    @DisplayName("List Prescriptions Tests")
    class ListTests {

        @Test
        @DisplayName("Should return list of patient prescriptions for doctor")
        void listForPatient_shouldReturnPrescriptionsForDoctor() {
            // Arrange
            Prescription p1 = createTestPrescription(UUID.randomUUID(), patientId);
            p1.setTitle("Prescription 1");
            Prescription p2 = createTestPrescription(UUID.randomUUID(), patientId);
            p2.setTitle("Prescription 2");

            when(authentication.getPrincipal()).thenReturn(doctorDetails);
            doReturn(doctorDetails.getAuthorities()).when(authentication).getAuthorities();
            when(authentication.isAuthenticated()).thenReturn(true);
            when(repository.findByPatientIdOrderByCreatedAtDesc(patientId))
                    .thenReturn(Arrays.asList(p1, p2));

            // Act
            List<PrescriptionResponse> result = prescriptionService.listForPatient(patientId);

            // Assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return patient's own prescriptions")
        void listForPatient_shouldReturnOwnPrescriptions() {
            // Arrange
            Prescription p1 = createTestPrescription(UUID.randomUUID(), patientId);

            when(authentication.getPrincipal()).thenReturn(patientDetails);
            doReturn(patientDetails.getAuthorities()).when(authentication).getAuthorities();
            when(authentication.isAuthenticated()).thenReturn(true);
            when(repository.findByPatientIdOrderByCreatedAtDesc(patientId))
                    .thenReturn(List.of(p1));

            // Act
            List<PrescriptionResponse> result = prescriptionService.listForPatient(patientId);

            // Assert
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should throw when prescription not found")
        void get_shouldThrowWhenNotFound() {
            // Arrange
            UUID prescriptionId = UUID.randomUUID();
            when(repository.findById(prescriptionId)).thenReturn(Optional.empty());

            // Act & Assert - no need to mock auth since findById returns empty first
            assertThatThrownBy(() -> prescriptionService.get(prescriptionId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Prescription not found");
        }
    }

    // ========== HELPER METHODS ==========

    private PrescriptionRequest createValidRequest() {
        PrescriptionRequest request = new PrescriptionRequest();
        request.setPatientId(patientId);
        request.setAppointmentId(UUID.randomUUID());
        request.setTitle("Antibiotic Treatment");
        request.setBody("Take as directed");
        request.setMedications(Arrays.asList("Amoxicillin", "Paracetamol"));
        return request;
    }

    private Prescription createTestPrescription(UUID prescriptionId, UUID patientId) {
        Prescription prescription = new Prescription();
        prescription.setId(prescriptionId);
        prescription.setPatientId(patientId);
        prescription.setDoctorId(doctorId);
        prescription.setTitle("Test Prescription");
        prescription.setBody("Test body");
        prescription.setMedications(List.of("testmed"));
        prescription.setInteractionWarnings(List.of());
        prescription.setCreatedAt(LocalDateTime.now());
        return prescription;
    }
}
