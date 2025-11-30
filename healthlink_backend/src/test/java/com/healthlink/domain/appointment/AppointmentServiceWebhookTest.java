package com.healthlink.domain.appointment;

import com.healthlink.domain.appointment.dto.AppointmentResponse;
import com.healthlink.domain.appointment.dto.CreateAppointmentRequest;
import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.appointment.service.AppointmentService;
import com.healthlink.domain.appointment.service.StaffAssignmentService;
import com.healthlink.domain.organization.entity.Facility;
import com.healthlink.domain.organization.repository.FacilityRepository;
import com.healthlink.domain.organization.repository.ServiceOfferingRepository;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.repository.DoctorRepository;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.domain.webhook.EventType;
import com.healthlink.domain.webhook.WebhookPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppointmentService webhook integration
 * 
 * Tests verify that webhook events are published correctly for:
 * - Appointment creation (APPOINTMENT_CREATED)
 * - Appointment cancellation (APPOINTMENT_CANCELED)
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceWebhookTest {

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
    private WebhookPublisherService webhookPublisher;

    @InjectMocks
    private AppointmentService appointmentService;

    private UUID appointmentId;
    private UUID doctorId;
    private UUID patientId;
    private UUID facilityId;
    private Appointment appointment;
    private Doctor doctor;
    private Patient patient;
    private Facility facility;
    private CreateAppointmentRequest createRequest;

    @BeforeEach
    void setUp() {
        appointmentId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        facilityId = UUID.randomUUID();

        // Setup patient
        patient = new Patient();
        patient.setId(patientId);
        patient.setEmail("patient@test.com");

        // Setup doctor
        doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setSlotDurationMinutes(30);
        doctor.setEarlyCheckinMinutes(15);

        // Setup facility
        facility = new Facility();
        facility.setId(facilityId);
        facility.setName("Test Facility");
        facility.setDoctorOwner(doctor);

        // Setup appointment
        appointment = new Appointment();
        appointment.setId(appointmentId);
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setFacility(facility);
        appointment.setAppointmentTime(LocalDateTime.now().plusDays(1));
        appointment.setEndTime(LocalDateTime.now().plusDays(1).plusMinutes(30));
        appointment.setStatus(AppointmentStatus.PENDING_PAYMENT);
        appointment.setReasonForVisit("Routine checkup");

        // Setup create request
        createRequest = new CreateAppointmentRequest();
        createRequest.setDoctorId(doctorId);
        createRequest.setFacilityId(facilityId);
        createRequest.setAppointmentTime(LocalDateTime.now().plusDays(1));
        createRequest.setReasonForVisit("Routine checkup");
    }

    @Test
    void createAppointment_shouldPublishAppointmentCreatedEvent() {
        // Arrange
        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(appointmentRepository.existsOverlappingAppointment(any(), any(), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        doNothing().when(webhookPublisher).publish(any(EventType.class), anyString());

        // Act
        AppointmentResponse result = appointmentService.createAppointment(createRequest, "patient@test.com");

        // Assert
        assertNotNull(result);
        assertEquals(appointmentId, result.getId());
        
        ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
        ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(webhookPublisher, times(1)).publish(eventTypeCaptor.capture(), referenceIdCaptor.capture());
        
        assertEquals(EventType.APPOINTMENT_CREATED, eventTypeCaptor.getValue());
        assertEquals(appointmentId.toString(), referenceIdCaptor.getValue());
    }

    @Test
    void cancel_shouldPublishAppointmentCanceledEvent() {
        // Arrange
        String cancelReason = "Patient requested cancellation";
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        doNothing().when(webhookPublisher).publish(any(EventType.class), anyString());

        // Act
        AppointmentResponse result = appointmentService.cancel(appointmentId, cancelReason);

        // Assert
        assertNotNull(result);
        
        ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
        ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(webhookPublisher, times(1)).publish(eventTypeCaptor.capture(), referenceIdCaptor.capture());
        
        assertEquals(EventType.APPOINTMENT_CANCELED, eventTypeCaptor.getValue());
        assertEquals(appointmentId.toString(), referenceIdCaptor.getValue());
    }

    @Test
    void cancel_withNullReason_shouldStillPublishEvent() {
        // Arrange
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        doNothing().when(webhookPublisher).publish(any(EventType.class), anyString());

        // Act
        AppointmentResponse result = appointmentService.cancel(appointmentId, null);

        // Assert
        assertNotNull(result);
        verify(webhookPublisher, times(1)).publish(eq(EventType.APPOINTMENT_CANCELED), eq(appointmentId.toString()));
    }

    @Test
    void cancel_whenAppointmentNotFound_shouldNotPublishEvent() {
        // Arrange
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> appointmentService.cancel(appointmentId, "test"));
        verify(webhookPublisher, never()).publish(any(EventType.class), anyString());
    }

    @Test
    void createAppointment_whenSaveFails_shouldNotPublishEvent() {
        // Arrange
        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(appointmentRepository.existsOverlappingAppointment(any(), any(), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> appointmentService.createAppointment(createRequest, "patient@test.com"));
        verify(webhookPublisher, never()).publish(any(EventType.class), anyString());
    }
}
