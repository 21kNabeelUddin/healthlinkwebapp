package com.healthlink.domain.appointment.service;

import com.healthlink.domain.appointment.dto.AppointmentResponse;
import com.healthlink.domain.appointment.dto.CreateAppointmentRequest;
import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.organization.entity.Facility;
import com.healthlink.domain.organization.repository.FacilityRepository;
import com.healthlink.domain.organization.repository.ServiceOfferingRepository;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.entity.Staff;
import com.healthlink.domain.user.repository.DoctorRepository;
import com.healthlink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AppointmentService
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private ServiceOfferingRepository serviceOfferingRepository;

    @Mock
    private StaffAssignmentService staffAssignmentService;

    @Mock
    private com.healthlink.domain.webhook.WebhookPublisherService webhookPublisherService;

    @InjectMocks
    private AppointmentService appointmentService;

    private Doctor testDoctor;
    private Patient testPatient;
    private Facility testFacility;
    private CreateAppointmentRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test doctor
        testDoctor = new Doctor();
        testDoctor.setId(UUID.randomUUID());
        testDoctor.setFirstName("Jane");
        testDoctor.setLastName("Smith");
        testDoctor.setSlotDurationMinutes(30);
        testDoctor.setEarlyCheckinMinutes(15);

        // Setup test patient
        testPatient = new Patient();
        testPatient.setId(UUID.randomUUID());
        testPatient.setEmail("patient@test.com");
        testPatient.setFirstName("John");
        testPatient.setLastName("Doe");

        testFacility = new Facility();
        testFacility.setId(UUID.randomUUID());
        testFacility.setDoctorOwner(testDoctor);
        testFacility.setActive(true);
        testFacility.setRequiresStaffAssignment(false);

        // Setup test request
        testRequest = new CreateAppointmentRequest();
        testRequest.setDoctorId(testDoctor.getId());
        testRequest.setFacilityId(testFacility.getId());
        testRequest.setAppointmentTime(LocalDateTime.now().plusDays(1));
        testRequest.setReasonForVisit("Regular checkup");
    }

    @Test
    void createAppointment_shouldCreateSuccessfully() {
        when(userRepository.findByEmail(testPatient.getEmail())).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(testDoctor.getId())).thenReturn(Optional.of(testDoctor));
        when(facilityRepository.findById(testFacility.getId())).thenReturn(Optional.of(testFacility));
        when(appointmentRepository.existsOverlappingAppointment(any(), any(), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> {
            Appointment a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AppointmentResponse response = appointmentService.createAppointment(testRequest, testPatient.getEmail());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.PENDING_PAYMENT.name());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void createAppointment_shouldThrowWhenPatientNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.createAppointment(testRequest, "unknown@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    void createAppointment_shouldThrowWhenDoctorNotFound() {
        when(userRepository.findByEmail(testPatient.getEmail())).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.createAppointment(testRequest, testPatient.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Doctor not found");
    }

    @Test
    void createAppointment_shouldThrowWhenTimeSlotOverlaps() {
        when(userRepository.findByEmail(testPatient.getEmail())).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(testDoctor.getId())).thenReturn(Optional.of(testDoctor));
        when(facilityRepository.findById(testFacility.getId())).thenReturn(Optional.of(testFacility));
        when(appointmentRepository.existsOverlappingAppointment(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> appointmentService.createAppointment(testRequest, testPatient.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void createAppointment_shouldAssignStaffWhenRequired() {
        testFacility.setRequiresStaffAssignment(true);
        Staff staff = new Staff();
        staff.setId(UUID.randomUUID());

        when(userRepository.findByEmail(testPatient.getEmail())).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(testDoctor.getId())).thenReturn(Optional.of(testDoctor));
        when(facilityRepository.findById(testFacility.getId())).thenReturn(Optional.of(testFacility));
        when(appointmentRepository.existsOverlappingAppointment(any(), any(), any())).thenReturn(false);
        when(staffAssignmentService.assignStaff(eq(testFacility.getId()), any(), any())).thenReturn(staff);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> {
            Appointment a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AppointmentResponse response = appointmentService.createAppointment(testRequest, testPatient.getEmail());

        assertThat(response.getAssignedStaffId()).isEqualTo(staff.getId());
        verify(staffAssignmentService).assignStaff(eq(testFacility.getId()), any(), any());
    }

    @Test
    void patientCheckIn_shouldUpdatePatientCheckInTime() {
        Appointment appointment = createTestAppointment();
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setAppointmentTime(LocalDateTime.now().plusMinutes(10));

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        appointmentService.patientCheckIn(appointment.getId(), testPatient.getEmail());

        assertThat(appointment.getPatientCheckInTime()).isNotNull();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
        assertThat(appointment.getCheckInTime()).isNotNull();
    }

    @Test
    void patientCheckIn_shouldThrowWhenTooEarly() {
        Appointment appointment = createTestAppointment();
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setAppointmentTime(LocalDateTime.now().plusHours(2));

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.patientCheckIn(appointment.getId(), testPatient.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Too early");
    }

    @Test
    void patientCheckIn_shouldThrowWhenNotConfirmed() {
        Appointment appointment = createTestAppointment();
        appointment.setStatus(AppointmentStatus.PENDING_PAYMENT);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.patientCheckIn(appointment.getId(), testPatient.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("must be confirmed");
    }

    @Test
    void staffCheckIn_shouldUpdateStaffCheckInTime() {
        Appointment appointment = createTestAppointment();
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setPatientCheckInTime(LocalDateTime.now());
        Staff staff = new Staff();
        staff.setId(UUID.randomUUID());
        appointment.setAssignedStaff(staff);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        appointmentService.staffCheckIn(appointment.getId(), staff.getId(), false);

        assertThat(appointment.getStaffCheckInTime()).isNotNull();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
    }

    @Test
    void staffCheckIn_shouldRejectUnassignedStaff() {
        Appointment appointment = createTestAppointment();
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Staff staff = new Staff();
        staff.setId(UUID.randomUUID());
        appointment.setAssignedStaff(staff);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.staffCheckIn(appointment.getId(), UUID.randomUUID(), false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("assigned staff");
    }

    @Test
    void reschedule_shouldUpdateAppointmentTime() {
        Appointment appointment = createTestAppointment();
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        LocalDateTime newTime = LocalDateTime.now().plusDays(2);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepository.existsOverlappingAppointment(any(), any(), any())).thenReturn(false);
        when(appointmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AppointmentResponse response = appointmentService.reschedule(
                appointment.getId(), newTime, testPatient.getEmail());

        assertThat(response.getStartTime()).isEqualTo(newTime);
    }

    @Test
    void reschedule_shouldThrowForUnauthorizedPatient() {
        Appointment appointment = createTestAppointment();

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.reschedule(
                appointment.getId(), LocalDateTime.now().plusDays(2), "unauthorized@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void cancel_shouldUpdateStatusToCancelled() {
        Appointment appointment = createTestAppointment();
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AppointmentResponse response = appointmentService.cancel(appointment.getId(), "Patient unavailable");

        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.CANCELLED.name());
    }

    @Test
    void completeAppointment_shouldUpdateStatusToCompleted() {
        Appointment appointment = createTestAppointment();
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AppointmentResponse response = appointmentService.completeAppointment(appointment.getId());

        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.COMPLETED.name());
    }

    @Test
    void listAppointments_shouldReturnPatientAppointments() {
        testPatient.setRole(com.healthlink.domain.user.enums.UserRole.PATIENT);
        List<Appointment> appointments = Arrays.asList(createTestAppointment(), createTestAppointment());

        when(userRepository.findByEmail(testPatient.getEmail())).thenReturn(Optional.of(testPatient));
        when(appointmentRepository.findByPatientId(testPatient.getId())).thenReturn(appointments);

        List<AppointmentResponse> result = appointmentService.listAppointments(testPatient.getEmail());

        assertThat(result).hasSize(2);
    }

    private Appointment createTestAppointment() {
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setDoctor(testDoctor);
        appointment.setPatient(testPatient);
        appointment.setFacility(testFacility);
        appointment.setAppointmentTime(LocalDateTime.now().plusDays(1));
        appointment.setEndTime(LocalDateTime.now().plusDays(1).plusMinutes(30));
        appointment.setStatus(AppointmentStatus.PENDING_PAYMENT);
        return appointment;
    }
}
