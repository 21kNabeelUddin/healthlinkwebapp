package com.healthlink.domain.user.controller;

import com.healthlink.domain.user.dto.UpdateProfileRequest;
import com.healthlink.domain.user.dto.UserDto;
import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.service.UserService;
import com.healthlink.security.model.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Operations for user profile management")
public class UserController {

    private final UserService userService;
    private final ModelMapper modelMapper;

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = userService.getUserById(principal.getId());
        return ResponseEntity.ok(modelMapper.map(user, UserDto.class));
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
