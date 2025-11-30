package com.healthlink.domain.appointment;

import com.healthlink.domain.appointment.dto.AppointmentResponse;
import com.healthlink.domain.appointment.dto.CreateAppointmentRequest;
import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.appointment.service.AppointmentService;
import com.healthlink.domain.organization.entity.Facility;
import com.healthlink.domain.organization.repository.FacilityRepository;
import com.healthlink.domain.organization.repository.ServiceOfferingRepository;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.repository.DoctorRepository;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.domain.appointment.service.StaffAssignmentService;
import com.healthlink.domain.webhook.WebhookPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentSchedulingTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private UserRepository userRepository;
    @Mock private WebhookPublisherService webhookPublisherService;
    @Mock private FacilityRepository facilityRepository;
    @Mock private ServiceOfferingRepository serviceOfferingRepository;
    @Mock private StaffAssignmentService staffAssignmentService;

    private AppointmentService service;

    private Doctor doctor;
    private Patient patient;
    private Facility facility;
    private final UUID doctorId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final String patientEmail = "patient@example.com";

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, doctorRepository, userRepository,
                facilityRepository, serviceOfferingRepository, staffAssignmentService, webhookPublisherService);

        doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setSlotDurationMinutes(30); // Default 30 mins
        doctor.setEarlyCheckinMinutes(15);

        patient = new Patient();
        patient.setId(patientId);
        patient.setEmail(patientEmail);

        facility = new Facility();
        facility.setId(UUID.randomUUID());
        facility.setDoctorOwner(doctor);
        facility.setActive(true);
        facility.setRequiresStaffAssignment(false);
    }

    @Test
    void shouldScheduleAppointmentWithCorrectDuration() {
        LocalDateTime startTime = LocalDateTime.of(2025, 11, 24, 10, 0);
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setDoctorId(doctorId);
        request.setFacilityId(facility.getId());
        request.setAppointmentTime(startTime);
        request.setReasonForVisit("Checkup");

        when(userRepository.findByEmail(patientEmail)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));
        when(appointmentRepository.existsOverlappingAppointment(eq(doctorId), eq(startTime), eq(startTime.plusMinutes(30))))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AppointmentResponse response = service.createAppointment(request, patientEmail);

        assertNotNull(response);
        assertEquals(startTime, response.getStartTime());
        assertEquals(startTime.plusMinutes(30), response.getEndTime());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void shouldAllowConsecutiveAppointments_NoBuffer() {
        // Existing appointment 10:00 - 10:30
        // New appointment 10:30 - 11:00
        LocalDateTime startTime = LocalDateTime.of(2025, 11, 24, 10, 30);
        
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setDoctorId(doctorId);
        request.setFacilityId(facility.getId());
        request.setAppointmentTime(startTime);

        when(userRepository.findByEmail(patientEmail)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));
        
        // Mock overlap check: 10:30 to 11:00 should NOT overlap with 10:00-10:30
        when(appointmentRepository.existsOverlappingAppointment(eq(doctorId), eq(startTime), eq(startTime.plusMinutes(30))))
                .thenReturn(false);
        
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        assertDoesNotThrow(() -> service.createAppointment(request, patientEmail));
    }

    @Test
    void shouldRejectOverlappingAppointments() {
        // New appointment 10:15 - 10:45 (Overlaps with hypothetical 10:00-10:30)
        LocalDateTime startTime = LocalDateTime.of(2025, 11, 24, 10, 15);
        
        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setDoctorId(doctorId);
        request.setFacilityId(facility.getId());
        request.setAppointmentTime(startTime);

        when(userRepository.findByEmail(patientEmail)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));
        
        when(appointmentRepository.existsOverlappingAppointment(eq(doctorId), eq(startTime), eq(startTime.plusMinutes(30))))
                .thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            service.createAppointment(request, patientEmail)
        );
        assertEquals("Doctor is not available at this time", exception.getMessage());
    }
}
