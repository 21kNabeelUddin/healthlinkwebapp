package com.healthlink.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Patient entity
 */
@Entity
@DiscriminatorValue("PATIENT")
@Getter
@Setter
@NoArgsConstructor
public class Patient extends User {
    
    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;
    
    @Column(name = "gender", length = 10)
    private String gender;  // MALE, FEMALE, OTHER
    
    @Column(name = "blood_group", length = 10)
    private String bloodGroup;
    
    @Column(name = "emergency_contact_name", length = 200)
    private String emergencyContactName;
    
    @Column (name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;
    
    @Column(name = "address", length = 500)
    private String address;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "country", length = 100)
    private String country = "Pakistan";

    @Column(name = "otp_enabled")
    private Boolean otpEnabled = false; // Per-patient toggle for requiring OTP in addition to password
}
