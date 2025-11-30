package com.healthlink.domain.record.service;

import com.healthlink.domain.record.dto.PrescriptionRequest;
import com.healthlink.domain.record.dto.PrescriptionResponse;
import com.healthlink.domain.record.entity.Prescription;
import com.healthlink.domain.record.entity.PrescriptionTemplate;
import com.healthlink.domain.record.repository.PrescriptionRepository;
import com.healthlink.domain.record.repository.PrescriptionTemplateRepository;
import com.healthlink.security.annotation.PhiAccess;
import com.healthlink.security.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import static com.healthlink.dto.mapper.DomainDtoMapper.toUtc;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository repository;
    private final PrescriptionTemplateRepository templateRepository;
    private final OpenFdaDrugInteractionClient interactionClient;

    @Transactional
    public PrescriptionResponse create(PrescriptionRequest request) {
        enforceDoctorRole();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID doctorId = null;
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            doctorId = cud.getId();
        }
        Prescription p = new Prescription();
        // Let Hibernate generate the UUID via @GeneratedValue - do NOT set manually
        // as it breaks version checking (Detached entity with uninitialized version)
        p.setPatientId(request.getPatientId());
        p.setAppointmentId(request.getAppointmentId());
        p.setDoctorId(doctorId); // may be null if principal missing (should not happen)
        p.setTitle(request.getTitle());
        String body = request.getBody();
        if (request.getTemplateId() != null) {
            PrescriptionTemplate template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new IllegalArgumentException("Template not found"));
            body = applyTemplate(template.getContent(), body);
        }
        p.setBody(body);
        List<String> meds = request.getMedications() == null ? List.of()
                : sanitizeMedications(request.getMedications());
        p.setMedications(meds);
        List<String> warnings = new ArrayList<>();
        for (String m : meds) {
            warnings.addAll(interactionClient.fetchInteractions(m));
        }
        p.setInteractionWarnings(warnings.stream().distinct().collect(Collectors.toList()));
        Prescription saved = repository.save(p);
        analyticsRecord(com.healthlink.analytics.AnalyticsEventType.PRESCRIPTION_CREATED, currentActor(),
                saved.getId().toString(), "patient=" + saved.getPatientId());
        return toResponse(saved);
    }

    @PhiAccess(reason = "prescription_view", entityType = Prescription.class, idParam = "id")
    @Transactional
    public PrescriptionResponse get(UUID id) {
        Prescription p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));
        enforceViewPermission(p.getPatientId());
        return toResponse(p);
    }

    @PhiAccess(reason = "prescription_list_patient", entityType = Prescription.class, idParam = "patientId")
    @Transactional
    public List<PrescriptionResponse> listForPatient(UUID patientId) {
        enforceViewPermission(patientId);
        return repository.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    private String applyTemplate(String templateContent, String body) {
        return templateContent.replace("{{body}}", body);
    }

    private List<String> sanitizeMedications(List<String> meds) {
        return meds.stream().map(m -> m.trim().toLowerCase()).filter(m -> !m.isBlank()).collect(Collectors.toList());
    }

    private void enforceDoctorRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"))) {
            throw new AccessDeniedException("Only doctors can create prescriptions");
        }
    }

    private void enforceViewPermission(UUID patientId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new AccessDeniedException("Unauthenticated");
        boolean isDoctor = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        boolean isPatient = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (auth.getPrincipal() instanceof CustomUserDetails cud) {
            if (isPatient && cud.getId().equals(patientId))
                return;
            if (isDoctor)
                return; // To refine with doctor-patient assignment rules
        }
        throw new AccessDeniedException("Forbidden");
    }

    private PrescriptionResponse toResponse(Prescription p) {
        // Force initialization of lazy collections before building response
        // to prevent LazyInitializationException after transaction closes
        List<String> meds = p.getMedications() != null ? new ArrayList<>(p.getMedications()) : List.of();
        List<String> warnings = p.getInteractionWarnings() != null ? new ArrayList<>(p.getInteractionWarnings()) : List.of();
        
        return PrescriptionResponse.builder()
                .id(p.getId())
                .patientId(p.getPatientId())
                .appointmentId(p.getAppointmentId())
                .doctorId(p.getDoctorId())
                .title(p.getTitle())
                .body(p.getBody())
                .medications(meds)
                .interactionWarnings(warnings)
                .createdAt(toUtc(p.getCreatedAt()))
                .build();
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
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    // Cache manager available for future cache eviction if needed
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @SuppressWarnings("unused")
    private org.springframework.cache.CacheManager cacheManager;
}
