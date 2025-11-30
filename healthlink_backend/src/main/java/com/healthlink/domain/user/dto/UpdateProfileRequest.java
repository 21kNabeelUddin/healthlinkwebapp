package com.healthlink.domain.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
    private String preferredLanguage;
    private String phone;
    private String dateOfBirth;
    private String gender;
    private String bloodType;
    private String address;
    private String city;
    private String emergencyContactName;
    private String emergencyContactPhone;
}
