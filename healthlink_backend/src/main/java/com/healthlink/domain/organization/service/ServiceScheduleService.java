package com.healthlink.domain.organization.service;

import com.healthlink.domain.organization.dto.ServiceScheduleRequest;
import com.healthlink.domain.organization.dto.ServiceScheduleResponse;
import com.healthlink.domain.organization.entity.ServiceOffering;
import com.healthlink.domain.organization.entity.ServiceSchedule;
import com.healthlink.domain.organization.repository.ServiceOfferingRepository;
import com.healthlink.domain.organization.repository.ServiceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceScheduleService {

    private final ServiceScheduleRepository scheduleRepository;
    private final ServiceOfferingRepository offeringRepository;

    public ServiceScheduleResponse create(ServiceScheduleRequest request) {
        ServiceOffering offering = offeringRepository.findById(request.getServiceOfferingId())
                .orElseThrow(() -> new IllegalArgumentException("Service offering not found"));
        var s = new ServiceSchedule();
        s.setServiceOffering(offering);
        s.setDayOfWeek(request.getDayOfWeek());
        s.setStartTime(request.getStartTime());
        s.setEndTime(request.getEndTime());
        return toDto(scheduleRepository.save(s));
    }

    public List<ServiceScheduleResponse> list(UUID offeringId) {
        return scheduleRepository.findByServiceOfferingId(offeringId).stream().map(this::toDto).toList();
    }

    public void delete(UUID id) {
        scheduleRepository.deleteById(id);
    }

    private ServiceScheduleResponse toDto(ServiceSchedule s) {
        return ServiceScheduleResponse.builder()
                .id(s.getId())
                .serviceOfferingId(s.getServiceOffering().getId())
                .dayOfWeek(s.getDayOfWeek())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .build();
    }
}