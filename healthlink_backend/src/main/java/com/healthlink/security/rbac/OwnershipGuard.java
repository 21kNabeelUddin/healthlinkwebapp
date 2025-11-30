package com.healthlink.security.rbac;

import com.healthlink.security.model.CustomUserDetails;
import com.healthlink.domain.record.repository.MedicalRecordRepository;
import com.healthlink.domain.record.repository.LabOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OwnershipGuard {
    private final MedicalRecordRepository medicalRecordRepository;
    private final LabOrderRepository labOrderRepository;

    public boolean isRecordOwner(UUID recordId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) return false;
        CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();
        return medicalRecordRepository.findById(recordId)
            .map(r -> r.getPatientId().equals(cud.getId()))
            .orElse(false);
    }

    public boolean isLabOrderOwner(UUID labOrderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) return false;
        CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();
        return labOrderRepository.findById(labOrderId)
                .map(o -> o.getPatientId().equals(cud.getId()))
                .orElse(false);
    }
}
