package com.healthlink.domain.record;

import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.record.dto.MedicalRecordRequest;
import com.healthlink.domain.record.entity.MedicalRecord;
import com.healthlink.domain.record.repository.MedicalRecordRepository;
import com.healthlink.domain.record.service.MedicalRecordService;
import com.healthlink.security.model.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MedicalRecordServiceTest {

    private MedicalRecordRepository repository;
    private AppointmentRepository appointmentRepository;
    private MedicalRecordService service;

    @BeforeEach
    void setup() {
        repository = Mockito.mock(MedicalRecordRepository.class);
        appointmentRepository = Mockito.mock(AppointmentRepository.class);
        service = new MedicalRecordService(repository, appointmentRepository);
    }

    @Test
    void patientCannotModifyOthersRecord() {
        UUID patientId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        var user = org.mockito.Mockito.mock(com.healthlink.domain.user.entity.User.class);
        when(user.getId()).thenReturn(patientId);
        when(user.getEmail()).thenReturn("patient@example.com");
        when(user.getRole()).thenReturn(com.healthlink.domain.user.enums.UserRole.PATIENT);
        when(user.getIsActive()).thenReturn(true);
        when(user.getIsEmailVerified()).thenReturn(true);
        when(user.getApprovalStatus()).thenReturn(com.healthlink.domain.user.enums.ApprovalStatus.APPROVED);
        var cud = new CustomUserDetails(user);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(cud, "", cud.getAuthorities()));
        MedicalRecordRequest req = new MedicalRecordRequest();
        req.setPatientId(otherId);
        req.setTitle("T");
        req.setDetails("D");
        assertThrows(Exception.class, () -> service.create(req));
    }

    @Test
    void doctorCanCreateRecordForPatient() {
        UUID patientId = UUID.randomUUID();
        var user = org.mockito.Mockito.mock(com.healthlink.domain.user.entity.User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getEmail()).thenReturn("doc@example.com");
        when(user.getRole()).thenReturn(com.healthlink.domain.user.enums.UserRole.DOCTOR);
        when(user.getIsActive()).thenReturn(true);
        when(user.getIsEmailVerified()).thenReturn(true);
        when(user.getApprovalStatus()).thenReturn(com.healthlink.domain.user.enums.ApprovalStatus.APPROVED);
        var cud = new CustomUserDetails(user);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(cud, "", cud.getAuthorities()));
        MedicalRecordRequest req = new MedicalRecordRequest();
        req.setPatientId(patientId);
        req.setTitle("Blood Test");
        req.setDetails("All vitals normal");
        when(repository.save(any(MedicalRecord.class))).thenAnswer(inv -> {
            MedicalRecord saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });
        assertDoesNotThrow(() -> service.create(req));
    }
}
