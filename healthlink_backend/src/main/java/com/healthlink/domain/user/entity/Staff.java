package com.healthlink.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Staff entity - assistants added by doctors or organizations
 */
@Entity
@DiscriminatorValue("STAFF")
@Getter
@Setter
@NoArgsConstructor
public class Staff extends User {
    
    @Column(name = "added_by_doctor_id")
    private UUID addedByDoctorId;
    
    @Column(name = "added_by_org_id")
    private UUID addedByOrgId;
    
    @Column(name = "assigned_facility_id")
    private UUID assignedFacilityId;
    
    @Column(name = "can_manage_appointments")
    private Boolean canManageAppointments = true;
    
    @Column(name = "can_record_payments")
    private Boolean canRecordPayments = true;
    
    @Column(name = "is_available")
    private Boolean isAvailable = true;
}
