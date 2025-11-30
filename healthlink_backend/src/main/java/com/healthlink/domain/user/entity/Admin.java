package com.healthlink.domain.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Admin entity - approves doctors and organizations
 * Created by Platform Owner
 */
@Entity
@DiscriminatorValue("ADMIN")
@Getter
@Setter
@NoArgsConstructor
public class Admin extends User {

    @NotNull
    @Column(name = "admin_username", unique = true, length = 100)
    private String adminUsername;

    @Column(name = "can_approve_doctors")
    private Boolean canApproveDoctors = true;

    @Column(name = "can_approve_organizations")
    private Boolean canApproveOrganizations = true;

    @Column(name = "can_view_analytics")
    private Boolean canViewAnalytics = true;

    @Column(name = "created_by_platform_owner_id")
    private java.util.UUID createdByPlatformOwnerId;
}
