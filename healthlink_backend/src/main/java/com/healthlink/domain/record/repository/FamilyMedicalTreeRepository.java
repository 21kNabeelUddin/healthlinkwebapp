package com.healthlink.domain.record.repository;

import com.healthlink.domain.record.entity.FamilyMedicalTree;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyMedicalTreeRepository extends JpaRepository<FamilyMedicalTree, UUID> {
    List<FamilyMedicalTree> findByPatientId(UUID patientId);
}
