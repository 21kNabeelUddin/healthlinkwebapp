package com.healthlink.domain.record.service;

import com.healthlink.domain.record.dto.MedicalRecordRequest;
import com.healthlink.domain.record.dto.MedicalRecordResponse;
import com.healthlink.domain.record.entity.MedicalRecord;
import com.healthlink.domain.record.repository.MedicalRecordRepository;
import com.healthlink.security.annotation.PhiAccess;
import com.healthlink.security.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import static com.healthlink.dto.mapper.DomainDtoMapper.toUtc;

/**
 * MedicalRecordService
 * Provides CRUD operations on unified MedicalRecord entity with PHI encryption.
 * HIPAA: Methods returning PHI annotated with {@link PhiAccess} for audit
 * logging.
 */
@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository repository;
    private final com.healthlink.domain.appointment.repository.AppointmentRepository appointmentRepository;

    public MedicalRecordResponse create(MedicalRecordRequest request) {
        enforcePatientOwnership(request.getPatientId());
        MedicalRecord record = new MedicalRecord();
        record.setPatientId(request.getPatientId());
        record.setTitle(request.getTitle());
        record.setSummary(request.getSummary());
        record.setDetails(request.getDetails());
        record.setAttachmentUrl(request.getAttachmentUrl());
        MedicalRecord saved = repository.save(record);
        analyticsRecord(com.healthlink.analytics.AnalyticsEventType.MEDICAL_RECORD_CREATED, currentActor(),
                saved.getId().toString(), "patient=" + saved.getPatientId());
        return toResponse(saved);
    }

    @PhiAccess(reason = "medical_record_view", entityType = MedicalRecord.class, idParam = "id")
    public MedicalRecordResponse get(UUID id) {
        MedicalRecord record = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found"));
        enforceViewPermission(record.getPatientId());
        analyticsRecord(com.healthlink.analytics.AnalyticsEventType.MEDICAL_RECORD_VIEW, currentActor(), id.toString(),
                "patient=" + record.getPatientId());
        return toResponse(record);
    }

    @PhiAccess(reason = "medical_record_list", entityType = MedicalRecord.class, idParam = "patientId")
    // Cache disabled for MVP - removed @Cacheable to avoid Redis dependency
    public List<MedicalRecordResponse> listForPatient(UUID patientId) {
        enforceViewPermission(patientId);
        return repository.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MedicalRecordResponse update(UUID id, MedicalRecordRequest request) {
        MedicalRecord record = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found"));
        enforcePatientOwnership(record.getPatientId());
        record.setTitle(request.getTitle());
        record.setSummary(request.getSummary());
        record.setDetails(request.getDetails());
        record.setAttachmentUrl(request.getAttachmentUrl());
        MedicalRecord saved = repository.save(record);
        analyticsRecord(com.healthlink.analytics.AnalyticsEventType.MEDICAL_RECORD_UPDATED, currentActor(),
                saved.getId().toString(), "patient=" + saved.getPatientId());
        evictCache(record.getPatientId());
        return toResponse(saved);
    }

    public void delete(UUID id) {
        MedicalRecord record = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found"));
        enforcePatientOwnership(record.getPatientId());
        repository.delete(record);
        analyticsRecord(com.healthlink.analytics.AnalyticsEventType.MEDICAL_RECORD_UPDATED, currentActor(),
                id.toString(), "deleted=true");
        evictCache(record.getPatientId());
    }

    private void enforcePatientOwnership(UUID patientId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            boolean isPatient = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
            if (isPatient && !cud.getId().equals(patientId)) {
                throw new AccessDeniedException("Cannot modify another patient's record");
            }
        }
    }

    private void enforceViewPermission(UUID patientId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new AccessDeniedException("Unauthenticated");
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isDoctor = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        boolean isPatient = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isAdmin)
            return; // Admin should ideally access only anonymized analytics; direct access allowed
                    // here only if policy permits.
        if (auth.getPrincipal() instanceof CustomUserDetails cud) {
            if (isPatient && cud.getId().equals(patientId))
                return;
            if (isDoctor) {
                // Allow if doctor has any appointment with this patient (past or future)
                boolean hasRelationship = appointmentRepository.existsByDoctorIdAndPatientId(cud.getId(), patientId);
                if (hasRelationship) {
                    return;
                }
            }
        }
        throw new AccessDeniedException("Forbidden");
    }

    private MedicalRecordResponse toResponse(MedicalRecord r) {
        return MedicalRecordResponse.builder()
                .id(r.getId())
                .patientId(r.getPatientId())
                .title(r.getTitle())
                .summary(r.getSummary())
                .details(r.getDetails())
                .attachmentUrl(r.getAttachmentUrl())
                .createdAt(toUtc(r.getCreatedAt()))
                .updatedAt(toUtc(r.getUpdatedAt()))
                .build();
    }

    // Added structured creation method required by EncounterController
    public MedicalRecordResponse createStructuredRecord(UUID patientId, UUID doctorId, String recordType,
            String details) {
        enforcePatientOwnership(patientId);
        MedicalRecord record = new MedicalRecord();
        record.setPatientId(patientId);
        record.setDoctorId(doctorId);
        record.setRecordType(recordType);
        record.setTitle(recordType);
        record.setDetails(details);
        MedicalRecord saved = repository.save(record);
        analyticsRecord(com.healthlink.analytics.AnalyticsEventType.MEDICAL_RECORD_CREATED, currentActor(),
                saved.getId().toString(), "structured=true");
        evictCache(patientId);
        return toResponse(saved);
    }

    public java.util.List<MedicalRecordResponse> getAllRecords(UUID patientId) {
        return listForPatient(patientId);
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.healthlink.analytics.AnalyticsEventService analyticsEventService;

    private void analyticsRecord(com.healthlink.analytics.AnalyticsEventType type, String actor, String subjectId,
            String meta) {
        if (analyticsEventService != null) {
            analyticsEventService.record(type, actor, subjectId, meta);
        }
    }

    private String currentActor() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.cache.CacheManager cacheManager;

    private void evictCache(UUID patientId) {
        if (cacheManager != null) {
            var c = cacheManager.getCache("medicalRecords");
            if (c != null)
                c.evict(patientId);
        }
    }
}
