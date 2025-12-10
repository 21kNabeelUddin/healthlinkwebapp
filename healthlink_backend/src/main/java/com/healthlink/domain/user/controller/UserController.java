package com.healthlink.domain.user.controller;

import com.healthlink.domain.user.dto.UpdateProfileRequest;
import com.healthlink.domain.user.dto.UserDto;
import com.healthlink.domain.user.dto.UserProfileResponse;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.service.UserService;
import com.healthlink.security.model.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Operations for user profile management")
public class UserController {

    private final UserService userService;
    private final ModelMapper modelMapper;

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = userService.getUserById(principal.getId());

        String specialization = null;
        String pmdcId = null;
        Integer yearsOfExperience = null;

        if (user instanceof Doctor doctor) {
            specialization = doctor.getSpecialization();
            pmdcId = doctor.getPmdcId();
            yearsOfExperience = doctor.getYearsOfExperience();
        }

        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .isEmailVerified(Boolean.TRUE.equals(user.getIsEmailVerified()))
                .isActive(Boolean.TRUE.equals(user.getIsActive()))
                .approvalStatus(user.getApprovalStatus())
                .specialization(specialization)
                .pmdcId(pmdcId)
                .yearsOfExperience(yearsOfExperience)
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update current user profile")
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody UpdateProfileRequest request) {
        User updated = userService.updateUser(
                principal.getId(),
                request.getFirstName(),
                request.getLastName(),
                request.getProfilePictureUrl(),
                request.getPreferredLanguage()
        );
        return ResponseEntity.ok(modelMapper.map(updated, UserDto.class));
    }
}
