package com.healthlink.domain.organization.repository;

import com.healthlink.domain.organization.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FacilityRepository extends JpaRepository<Facility, UUID> {
    List<Facility> findByOrganizationId(UUID organizationId);
    List<Facility> findByDoctorOwnerId(UUID doctorId);
    Integer countByOrganizationId(UUID organizationId);
}