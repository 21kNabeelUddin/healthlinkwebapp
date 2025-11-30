package com.healthlink.domain.analytics.service;

import com.healthlink.domain.analytics.dto.OrganizationAnalyticsResponse;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.organization.repository.FacilityRepository;
import com.healthlink.domain.organization.repository.ServiceOfferingRepository;
import com.healthlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationAnalyticsService {

    private final UserRepository userRepository; // retained for future enhancement
    private final FacilityRepository facilityRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final AppointmentRepository appointmentRepository; // retained for future enhancement

    public OrganizationAnalyticsResponse getOrganizationAnalytics(UUID organizationId) {
        Integer totalDoctors = userRepository.countDoctorsByOrganization(organizationId);
        Integer totalStaff = userRepository.countStaffByOrganization(organizationId);
        Integer totalFacilities = facilityRepository.countByOrganizationId(organizationId);
        Integer totalServices = serviceOfferingRepository.countByOrganizationId(organizationId);
        Integer totalAppointments = appointmentRepository.countByOrganizationId(organizationId);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Integer activeDoctors = appointmentRepository.countDistinctDoctorsByOrganizationIdAndAfter(organizationId, thirtyDaysAgo);

        return OrganizationAnalyticsResponse.builder()
                .totalDoctors(totalDoctors)
                .totalStaff(totalStaff)
                .totalFacilities(totalFacilities)
                .totalServices(totalServices)
                .totalAppointments(totalAppointments)
                .activeDoctors(activeDoctors)
                .build();
    }
}
