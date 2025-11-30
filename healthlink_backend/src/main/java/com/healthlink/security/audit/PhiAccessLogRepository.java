package com.healthlink.security.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhiAccessLogRepository extends JpaRepository<PhiAccessLog, Long> {
}
