package com.healthlink.domain.record.repository;

import com.healthlink.domain.record.entity.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabOrderRepository extends JpaRepository<LabOrder, UUID> {
    List<LabOrder> findByPatientId(UUID patientId);
}
