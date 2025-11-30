package com.healthlink.domain.review.controller;

import com.healthlink.domain.review.dto.CreateReviewRequest;
import com.healthlink.domain.review.dto.ReviewResponse;
import com.healthlink.domain.review.service.ReviewService;
import com.healthlink.security.model.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ReviewResponse create(@Valid @RequestBody CreateReviewRequest request, Authentication auth) {
        CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();
        return reviewService.create(request, cud.getId());
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN','STAFF','ORGANIZATION')")
    public List<ReviewResponse> forDoctor(@PathVariable UUID doctorId) {
        return reviewService.forDoctor(doctorId);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<ReviewResponse> mine(Authentication auth) {
        CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();
        return reviewService.mine(cud.getId());
    }
}
