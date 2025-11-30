package com.healthlink.domain.review.repository;

import com.healthlink.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByDoctorId(UUID doctorId);
    List<Review> findByPatientId(UUID patientId);
}
