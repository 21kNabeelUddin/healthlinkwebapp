package com.healthlink.domain.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Platform Owner - highest level admin
 * Manages admin accounts and system-wide settings
 */
@Entity
@DiscriminatorValue("PLATFORM_OWNER")
@Getter
@Setter
@NoArgsConstructor
public class PlatformOwner extends User {

    @NotNull
    @Column(name = "owner_username", unique = true, length = 100)
    private String ownerUsername;

    @Column(name = "has_full_access")
    private Boolean hasFullAccess = true;
}
