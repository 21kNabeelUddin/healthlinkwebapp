package com.healthlink.security.service;

import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.security.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Central authorization helper for doctor-patient assignment checks based on existing appointments.
 * A doctor is considered "assigned" to a patient if at least one non-cancelled appointment exists between them.
 */
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final AppointmentRepository appointmentRepository;

    public boolean doctorAssignedToPatient(UUID patientId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails cud)) return false;
        boolean isDoctor = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (!isDoctor) return false;
        // Simple heuristic: doctor has at least one appointment with patient
        return appointmentRepository.findByPatientId(patientId).stream().anyMatch(a -> a.getDoctor().getId().equals(cud.getId()));
    }

    public boolean currentUserIsPatient(UUID patientId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails cud)) return false;
        boolean isPatient = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        return isPatient && cud.getId().equals(patientId);
    }
}
