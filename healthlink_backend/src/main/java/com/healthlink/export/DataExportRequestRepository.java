package com.healthlink.export;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DataExportRequestRepository extends JpaRepository<DataExportRequest, UUID> {
    List<DataExportRequest> findTop20ByOrderByRequestedAtDesc();
    boolean existsByStatus(String status);
}
