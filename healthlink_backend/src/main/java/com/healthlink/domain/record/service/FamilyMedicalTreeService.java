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
public class FamilyMedicalTreeService {
    private static final Logger logger = LoggerFactory.getLogger(FamilyMedicalTreeService.class);

    private final FamilyMedicalTreeRepository repository;

    public FamilyMedicalTreeResponse addEntry(UUID patientId, FamilyMedicalTreeRequest request) {
        FamilyMedicalTree node = new FamilyMedicalTree();
        node.setPatientId(patientId);
        node.setRelativeName(request.getRelativeName());
        node.setRelationship(request.getRelationship());
        node.setCondition(request.getCondition());
        node.setDiagnosedAt(request.getDiagnosedAt());
        node.setNotes(request.getNotes());
        node.setDeceased(request.getDeceased());
        node.setAgeAtDiagnosis(request.getAgeAtDiagnosis());
        
        FamilyMedicalTree saved = repository.save(node);
        logger.info("Added family history for patient {}: {} ({})", patientId, request.getRelativeName(), request.getRelationship());
        return toResponse(saved);
    }

    public FamilyMedicalTreeResponse updateEntry(UUID memberId, FamilyMedicalTreeRequest request) {
        FamilyMedicalTree node = repository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Family history not found: " + memberId));
        
        node.setRelativeName(request.getRelativeName());
        node.setRelationship(request.getRelationship());
        node.setCondition(request.getCondition());
        node.setDiagnosedAt(request.getDiagnosedAt());
        node.setNotes(request.getNotes());
        node.setDeceased(request.getDeceased());
        node.setAgeAtDiagnosis(request.getAgeAtDiagnosis());
        
        FamilyMedicalTree updated = repository.save(node);
        logger.info("Updated family history {}: {} ({})", memberId, request.getRelativeName(), request.getRelationship());
        return toResponse(updated);
    }

    public List<FamilyMedicalTreeResponse> forPatient(UUID patientId) {
        return repository.findByPatientId(patientId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Family history not found: " + id);
        }
        repository.deleteById(id);
        logger.info("Deleted family history {}", id);
    }

    private FamilyMedicalTreeResponse toResponse(FamilyMedicalTree node) {
        return FamilyMedicalTreeResponse.builder()
                .id(node.getId())
                .patientId(node.getPatientId())
                .relativeName(node.getRelativeName())
                .relationship(node.getRelationship())
                .condition(node.getCondition())
                .diagnosedAt(node.getDiagnosedAt())
            .notes(node.getNotes())
            .deceased(node.getDeceased())
            .ageAtDiagnosis(node.getAgeAtDiagnosis())
                .build();
    }
}
