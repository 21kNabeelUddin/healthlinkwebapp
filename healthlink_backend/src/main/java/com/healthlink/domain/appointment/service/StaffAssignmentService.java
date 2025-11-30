package com.healthlink.domain.appointment.service;

import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.user.entity.Staff;
import com.healthlink.domain.user.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffAssignmentService {

    private final StaffRepository staffRepository;
    private final AppointmentRepository appointmentRepository;

    public Staff assignStaff(UUID facilityId, LocalDateTime startTime, LocalDateTime endTime) {
        return assignStaff(facilityId, startTime, endTime, null);
    }

    public Staff assignStaff(UUID facilityId, LocalDateTime startTime, LocalDateTime endTime, UUID excludeAppointmentId) {
        List<Staff> candidates = staffRepository.findAvailableByFacility(facilityId);
        for (Staff candidate : candidates) {
            if (candidate.getId() == null) {
                continue;
            }
            boolean hasConflict = appointmentRepository
                    .staffHasConflictingAppointment(candidate.getId(), startTime, endTime, excludeAppointmentId);
            if (!hasConflict) {
                return candidate;
            }
        }
        throw new IllegalStateException("No staff available for the selected slot");
    }
}
