package com.healthlink.domain.organization.service;

import com.healthlink.domain.organization.dto.FacilityRequest;
import com.healthlink.domain.organization.dto.FacilityResponse;
import com.healthlink.domain.organization.entity.Facility;
import com.healthlink.domain.organization.repository.FacilityRepository;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Organization;
import com.healthlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final UserRepository userRepository;

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