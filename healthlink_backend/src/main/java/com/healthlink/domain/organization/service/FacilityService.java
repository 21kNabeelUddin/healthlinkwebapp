package com.healthlink.domain.organization.service;

import com.healthlink.domain.organization.dto.FacilityRequest;
import com.healthlink.domain.organization.dto.FacilityResponse;
import com.healthlink.domain.organization.entity.Facility;
import com.healthlink.domain.organization.repository.FacilityRepository;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.notification.NotificationType;
import com.healthlink.domain.notification.service.NotificationSchedulerService;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Organization;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.service.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationSchedulerService notificationSchedulerService;
    private final EmailService emailService;

    public FacilityResponse createForOrganization(UUID organizationId, FacilityRequest request) {
        Organization org = (Organization) userRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        var f = new Facility();
        f.setOrganization(org);
        mapRequestToEntity(request, f);
        f.setActive(true);
        return toDto(facilityRepository.save(f));
    }

    public FacilityResponse createForDoctor(UUID doctorId, FacilityRequest request) {
        Doctor doc = (Doctor) userRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        var f = new Facility();
        f.setDoctorOwner(doc);
        mapRequestToEntity(request, f);
        f.setActive(true);
        return toDto(facilityRepository.save(f));
    }

    public List<FacilityResponse> listForOrganization(UUID organizationId) {
        return facilityRepository.findByOrganizationId(organizationId).stream().map(this::toDto).toList();
    }

    public List<FacilityResponse> listForDoctor(UUID doctorId) {
        return facilityRepository.findByDoctorOwnerId(doctorId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<FacilityResponse> listAll() {
        return facilityRepository.findAll().stream()
                .filter(f -> f.getDeletedAt() == null)
                .map(this::toDto)
                .toList();
    }

    public FacilityResponse update(UUID id, FacilityRequest request) {
        Facility f = facilityRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Facility not found"));
        mapRequestToEntity(request, f);
        return toDto(facilityRepository.save(f));
    }

    public void deactivate(UUID id) {
        Facility f = facilityRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Facility not found"));
        f.setActive(false);
        facilityRepository.save(f);
    }

    public void deleteFacility(UUID id) {
        Facility f = facilityRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Facility not found"));

        // Cancel all appointments linked to this facility
        var appointments = appointmentRepository.findByFacilityId(id);
        for (var apt : appointments) {
            apt.setStatus(AppointmentStatus.CANCELLED);
            apt.setDeletedAt(LocalDateTime.now());
        }
        appointmentRepository.saveAll(appointments);

        // Soft-delete facility
        f.softDelete();
        f.setActive(false);
        facilityRepository.save(f);

        // Notify doctor owner (in-app + email)
        Doctor doctor = f.getDoctorOwner();
        if (doctor != null) {
            try {
                notificationSchedulerService.scheduleNotification(
                        doctor.getId(),
                        NotificationType.APPOINTMENT_CANCELED,
                        "Clinic deleted",
                        "Your clinic \"" + f.getName() + "\" was deleted by an admin. Related appointments were cancelled."
                );
            } catch (Exception ignored) {
            }

            if (doctor.getEmail() != null) {
                try {
                    emailService.sendSimpleEmail(
                            doctor.getEmail(),
                            "Clinic deleted by admin",
                            "Your clinic \"" + f.getName() + "\" was deleted by an admin. All related appointments were cancelled."
                    );
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void activate(UUID id) {
        Facility f = facilityRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Facility not found"));
        f.setActive(true);
        facilityRepository.save(f);
    }

    private FacilityResponse toDto(Facility f) {
        return FacilityResponse.builder()
                .id(f.getId())
                .name(f.getName())
                .address(f.getAddress())
                .town(f.getTown())
                .city(f.getCity())
                .state(f.getState())
                .zipCode(f.getZipCode())
                .phoneNumber(f.getPhoneNumber())
                .email(f.getEmail())
                .description(f.getDescription())
                .openingTime(f.getOpeningTime())
                .closingTime(f.getClosingTime())
                .latitude(f.getLatitude())
                .longitude(f.getLongitude())
                .consultationFee(f.getConsultationFee())
                .active(f.isActive())
                .organizationId(f.getOrganization() != null ? f.getOrganization().getId() : null)
                .doctorOwnerId(f.getDoctorOwner() != null ? f.getDoctorOwner().getId() : null)
                .servicesOffered(f.getServicesOffered())
                .build();
    }

    private void mapRequestToEntity(FacilityRequest request, Facility facility) {
        facility.setName(request.getName());
        facility.setAddress(request.getAddress());
        facility.setTown(request.getTown());
        facility.setCity(request.getCity());
        facility.setState(request.getState());
        facility.setZipCode(request.getZipCode());
        facility.setPhoneNumber(request.getPhoneNumber());
        facility.setEmail(request.getEmail());
        facility.setDescription(request.getDescription());
        facility.setOpeningTime(request.getOpeningTime());
        facility.setClosingTime(request.getClosingTime());
        facility.setLatitude(request.getLatitude());
        facility.setLongitude(request.getLongitude());
        facility.setConsultationFee(request.getConsultationFee());
        facility.setServicesOffered(request.getServicesOffered());
    }
}