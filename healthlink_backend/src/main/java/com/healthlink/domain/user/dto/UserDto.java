package com.healthlink.domain.user.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserDto {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String role;
    private String profilePictureUrl;
    private String preferredLanguage;
    private boolean isEmailVerified;
}
