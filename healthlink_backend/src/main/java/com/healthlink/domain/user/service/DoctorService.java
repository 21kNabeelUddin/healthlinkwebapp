package com.healthlink.domain.user.service;

import com.healthlink.domain.user.dto.DoctorDashboardDTO;
import java.util.UUID;

public interface DoctorService {
    DoctorDashboardDTO getDashboard(UUID doctorId);

    com.healthlink.domain.user.entity.Doctor getDoctorById(UUID doctorId);

    java.util.List<com.healthlink.domain.user.entity.Doctor> searchDoctors(String query);
}
