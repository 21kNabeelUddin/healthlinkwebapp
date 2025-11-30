package com.healthlink.domain.record.service;

import com.healthlink.domain.record.dto.FamilyMedicalTreeResponse;
import com.healthlink.domain.record.dto.FamilyMedicalTreeRequest;
import com.healthlink.domain.record.entity.FamilyMedicalTree;
import com.healthlink.domain.record.repository.FamilyMedicalTreeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FamilyHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(FamilyHistoryService.class);

    private final FamilyMedicalTreeRepository repository;

    public FamilyMedicalTreeResponse addFamilyHistory(UUID patientId, FamilyMedicalTreeRequest request) {
        FamilyMedicalTree entry = new FamilyMedicalTree();
        entry.setPatientId(patientId);
        entry.setRelativeName(request.getRelativeName());
        entry.setRelationship(request.getRelationship());
        entry.setCondition(request.getCondition());
        entry.setDiagnosedAt(request.getDiagnosedAt());
        entry.setNotes(request.getNotes());
        entry.setDeceased(request.getDeceased());
        entry.setAgeAtDiagnosis(request.getAgeAtDiagnosis());
        
        FamilyMedicalTree saved = repository.save(entry);
        logger.info("Added family history for patient {}: {} ({})", patientId, request.getRelativeName(), request.getRelationship());
        return toResponse(saved);
    }

    public FamilyMedicalTreeResponse updateFamilyHistory(UUID memberId, FamilyMedicalTreeRequest request) {
        FamilyMedicalTree entry = repository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Family history not found: " + memberId));
        
        entry.setRelativeName(request.getRelativeName());
        entry.setRelationship(request.getRelationship());
        entry.setCondition(request.getCondition());
        entry.setDiagnosedAt(request.getDiagnosedAt());
        entry.setNotes(request.getNotes());
        entry.setDeceased(request.getDeceased());
        entry.setAgeAtDiagnosis(request.getAgeAtDiagnosis());
        
        FamilyMedicalTree updated = repository.save(entry);
        logger.info("Updated family history {}: {} ({})", memberId, request.getRelativeName(), request.getRelationship());
        return toResponse(updated);
    }

    public void deleteFamilyHistory(UUID memberId) {
        if (!repository.existsById(memberId)) {
            throw new IllegalArgumentException("Family history not found: " + memberId);
        }
        repository.deleteById(memberId);
        logger.info("Deleted family history {}", memberId);
    }

    public List<FamilyMedicalTreeResponse> getHistoryByPatient(UUID patientId) {
        return repository.findByPatientId(patientId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private FamilyMedicalTreeResponse toResponse(FamilyMedicalTree entry) {
        return FamilyMedicalTreeResponse.builder()
                .id(entry.getId())
                .patientId(entry.getPatientId())
                .relativeName(entry.getRelativeName())
                .relationship(entry.getRelationship())
                .condition(entry.getCondition())
                .diagnosedAt(entry.getDiagnosedAt())
            .notes(entry.getNotes())
            .deceased(entry.getDeceased())
            .ageAtDiagnosis(entry.getAgeAtDiagnosis())
                .build();
    }
}
