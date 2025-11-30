package com.healthlink.domain.review.service;

import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.review.dto.CreateReviewRequest;
import com.healthlink.domain.review.dto.ReviewResponse;
import com.healthlink.domain.review.entity.Review;
import com.healthlink.domain.review.repository.ReviewRepository;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    public ReviewResponse create(CreateReviewRequest request, UUID patientId) {
        Appointment appt = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        // Ensure the authenticated patient matches the appointment's patient
        if (appt.getPatient() == null || !patientId.equals(appt.getPatient().getId())) {
            throw new IllegalStateException("Appointment does not belong to authenticated patient");
        }
        Review review = new Review();
        review.setAppointmentId(request.appointmentId());
        review.setDoctorId(request.doctorId());
        review.setPatientId(patientId);
        review.setRating(request.rating());
        review.setComments(request.comments());
        reviewRepository.save(review);

        // Update doctor aggregates
        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        int total = doctor.getTotalReviews() == null ? 0 : doctor.getTotalReviews();
        double avg = doctor.getAverageRating() == null ? 0.0 : doctor.getAverageRating();
        double newAvg = ((avg * total) + request.rating()) / (total + 1);
        doctor.setTotalReviews(total + 1);
        doctor.setAverageRating(newAvg);
        doctorRepository.save(doctor);

        return toResponse(review);
    }

    public List<ReviewResponse> forDoctor(UUID doctorId) {
        return reviewRepository.findByDoctorId(doctorId).stream().map(this::toResponse).toList();
    }

    public List<ReviewResponse> mine(UUID patientId) {
        return reviewRepository.findByPatientId(patientId).stream().map(this::toResponse).toList();
    }

    private ReviewResponse toResponse(Review r) {
        // Get reviewer name from patient (masked for privacy)
        String reviewerName = r.getPatientId() != null ? "Patient-" + r.getPatientId().toString().substring(0, 8)
                : "Anonymous";
        return new ReviewResponse(
                r.getId(),
                r.getDoctorId(),
                r.getPatientId(),
                r.getAppointmentId(),
                r.getRating(),
                r.getComments(),
                reviewerName,
                r.getIsReported() != null ? r.getIsReported() : false,
                r.getReportReason(),
                null, // response - not implemented in v1
                null, // respondedAt - not implemented in v1
                r.getCreatedAt());
    }
}
