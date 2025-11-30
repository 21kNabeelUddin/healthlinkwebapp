package com.healthlink.domain.organization.repository;

import com.healthlink.domain.organization.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {
    List<ServiceOffering> findByFacilityId(UUID facilityId);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM ServiceOffering s WHERE s.facility.organization.id = :orgId")
    Integer countByOrganizationId(@org.springframework.data.repository.query.Param("orgId") UUID organizationId);
}