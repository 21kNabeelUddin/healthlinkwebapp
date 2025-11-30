package com.healthlink.domain.record.repository;

import com.healthlink.domain.record.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {
    List<MedicalRecord> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
    List<MedicalRecord> findByPatientId(UUID patientId);
    List<MedicalRecord> findByPatientIdAndRecordType(UUID patientId, String recordType);
}
