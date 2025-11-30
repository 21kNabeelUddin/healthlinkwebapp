package com.healthlink.domain.user.repository;

import com.healthlink.domain.user.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StaffRepository extends JpaRepository<Staff, UUID> {

    @Query("""
            SELECT s FROM Staff s
            WHERE s.assignedFacilityId = :facilityId
              AND s.isAvailable = true
              AND s.isActive = true
            ORDER BY CASE WHEN s.lastLoginAt IS NULL THEN 1 ELSE 0 END, s.lastLoginAt DESC
            """)
    List<Staff> findAvailableByFacility(@Param("facilityId") UUID facilityId);
}
