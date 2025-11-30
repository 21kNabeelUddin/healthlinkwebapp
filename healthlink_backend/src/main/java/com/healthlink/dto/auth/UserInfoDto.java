package com.healthlink.dto.auth;

import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.enums.UserRole;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {
    
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private UserRole role;
    private ApprovalStatus approvalStatus;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private String profilePictureUrl;
    private String preferredLanguage;
}
