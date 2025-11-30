package com.healthlink.domain.organization.service;

import com.healthlink.domain.organization.dto.ServiceOfferingRequest;
import com.healthlink.domain.organization.dto.ServiceOfferingResponse;
import com.healthlink.domain.organization.entity.Facility;
import com.healthlink.domain.organization.entity.ServiceOffering;
import com.healthlink.domain.organization.repository.FacilityRepository;
import com.healthlink.domain.organization.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceOfferingService {

    private final ServiceOfferingRepository offeringRepository;
    private final FacilityRepository facilityRepository;

    public ServiceOfferingResponse create(ServiceOfferingRequest request) {
        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));
        var o = new ServiceOffering();
        o.setFacility(facility);
        o.setName(request.getName());
        o.setDescription(request.getDescription());
        o.setBaseFee(request.getBaseFee());
        o.setDurationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 15);
        o.setRequiresStaffAssignment(Boolean.TRUE.equals(request.getRequiresStaffAssignment()));
        return toDto(offeringRepository.save(o));
    }

    public List<ServiceOfferingResponse> list(UUID facilityId) {
        return offeringRepository.findByFacilityId(facilityId).stream().map(this::toDto).toList();
    }

    public ServiceOfferingResponse update(UUID id, ServiceOfferingRequest request) {
        ServiceOffering o = offeringRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service offering not found"));
        o.setName(request.getName());
        o.setDescription(request.getDescription());
        o.setBaseFee(request.getBaseFee());
        o.setDurationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 15);
        o.setRequiresStaffAssignment(Boolean.TRUE.equals(request.getRequiresStaffAssignment()));
        return toDto(offeringRepository.save(o));
    }

    public void delete(UUID id) {
        offeringRepository.deleteById(id);
    }

    private ServiceOfferingResponse toDto(ServiceOffering o) {
        return ServiceOfferingResponse.builder()
                .id(o.getId())
                .facilityId(o.getFacility().getId())
                .name(o.getName())
                .description(o.getDescription())
                .baseFee(o.getBaseFee())
                .durationMinutes(o.getDurationMinutes())
                .requiresStaffAssignment(o.getRequiresStaffAssignment())
                .build();
    }
}