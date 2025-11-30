package com.healthlink.domain.organization.repository;

import com.healthlink.domain.organization.entity.ServiceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ServiceScheduleRepository extends JpaRepository<ServiceSchedule, UUID> {
    List<ServiceSchedule> findByServiceOfferingId(UUID offeringId);
}