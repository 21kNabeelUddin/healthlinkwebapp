package com.healthlink.domain.user.service;

import com.healthlink.domain.appointment.entity.PaymentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import com.healthlink.domain.appointment.repository.PaymentRepository;
import com.healthlink.domain.appointment.service.AppointmentService;
import com.healthlink.domain.user.dto.DoctorDashboardDTO;
import com.healthlink.domain.user.dto.CreateEmergencyPatientRequest;
import com.healthlink.domain.user.dto.EmergencyPatientResponse;
import com.healthlink.domain.user.dto.CreateEmergencyPatientAndAppointmentRequest;
import com.healthlink.domain.user.dto.EmergencyPatientAndAppointmentResponse;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.DoctorRepository;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.service.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorServiceImpl implements DoctorService {

        private final DoctorRepository doctorRepository;
        private final PaymentRepository paymentRepository;
        private final AppointmentRepository appointmentRepository;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final AppointmentService appointmentService;
        private final EmailService emailService;
        
        private static final SecureRandom RANDOM = new SecureRandom();

        @Override
        public DoctorDashboardDTO getDashboard(UUID doctorId) {
                Doctor doctor = doctorRepository.findById(doctorId)
                                .orElseThrow(() -> new RuntimeException("Doctor not found"));

                BigDecimal verified = paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.VERIFIED);
                BigDecimal captured = paymentRepository.sumAmountByDoctorIdAndStatus(doctorId, PaymentStatus.CAPTURED);

                BigDecimal totalRevenue = (verified != null ? verified : BigDecimal.ZERO)
                                .add(captured != null ? captured : BigDecimal.ZERO);

                Integer totalAppointments = appointmentRepository.countByDoctorId(doctorId);

                return DoctorDashboardDTO.builder()
                                .totalRevenue(totalRevenue)
                                .totalAppointments(totalAppointments)
                                .averageRating(doctor.getAverageRating())
                                .totalReviews(doctor.getTotalReviews())
                                .build();
        }

        @Override
        public Doctor getDoctorById(UUID doctorId) {
                return doctorRepository.findById(doctorId)
                                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        }

        @Override
        public java.util.List<Doctor> searchDoctors(String query) {
                return doctorRepository.searchDoctors(query);
        }
        
        @Override
        @Transactional
        public EmergencyPatientResponse createEmergencyPatient(UUID doctorId, CreateEmergencyPatientRequest request) {
                // Verify doctor exists
                doctorRepository.findById(doctorId)
                                .orElseThrow(() -> new RuntimeException("Doctor not found"));
                
                // Use patient's real email from request
                String email = request.getEmail().trim().toLowerCase();
                
                // Check if email already exists
                if (userRepository.existsByEmail(email)) {
                        throw new RuntimeException("An account with this email already exists");
                }
                
                // Generate temporary password (not returned to frontend, patient will reset it)
                String temporaryPassword = generateTemporaryPassword();
                
                // Set patient name - split if possible, otherwise use full name
                String patientName = request.getPatientName().trim();
                String[] nameParts = patientName.split("\\s+", 2);
                
                // Create patient
                Patient patient = new Patient();
                patient.setEmail(email);
                patient.setPasswordHash(passwordEncoder.encode(temporaryPassword));
                
                if (nameParts.length >= 2) {
                        patient.setFirstName(nameParts[0]);
                        patient.setLastName(nameParts[1]);
                } else {
                        patient.setFirstName(patientName);
                        patient.setLastName("");
                }
                
                patient.setPhoneNumber(request.getPhoneNumber());
                patient.setRole(UserRole.PATIENT);
                patient.setApprovalStatus(ApprovalStatus.APPROVED); // Auto-approved
                patient.setIsEmailVerified(true); // Auto-verified
                patient.setIsActive(true);
                patient.setPreferredLanguage("en");
                
                Patient savedPatient = userRepository.save(patient);
                
                // Send welcome email with password reset instructions
                emailService.sendEmergencyPatientWelcomeEmail(email, patientName);
                
                return EmergencyPatientResponse.builder()
                                .patientId(savedPatient.getId())
                                .email(email)
                                .patientName(patientName)
                                .phoneNumber(request.getPhoneNumber())
                                .build();
        }
        
        @Override
        @Transactional
        public EmergencyPatientAndAppointmentResponse createEmergencyPatientAndAppointment(
                        UUID doctorId, CreateEmergencyPatientAndAppointmentRequest request) {
                // Create emergency patient first
                CreateEmergencyPatientRequest patientRequest = new CreateEmergencyPatientRequest();
                patientRequest.setPatientName(request.getPatientName());
                patientRequest.setEmail(request.getEmail()); // Pass email from request
                patientRequest.setPhoneNumber(request.getPhoneNumber());
                
                EmergencyPatientResponse patientResponse = createEmergencyPatient(doctorId, patientRequest);
                
                // Mark appointment as emergency
                request.getAppointmentRequest().setIsEmergency(true);
                
                // Create appointment using the patient's email
                var appointmentResponse = appointmentService.createAppointment(
                                request.getAppointmentRequest(), 
                                patientResponse.getEmail());
                
                return EmergencyPatientAndAppointmentResponse.builder()
                                .patient(patientResponse)
                                .appointment(appointmentResponse)
                                .build();
        }
        
        private String generateTemporaryPassword() {
                // Generate a secure random password: 12 characters with uppercase, lowercase, numbers, and special chars
                String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
                String lowercase = "abcdefghijklmnopqrstuvwxyz";
                String numbers = "0123456789";
                String special = "!@#$%^&*";
                String allChars = uppercase + lowercase + numbers + special;
                
                StringBuilder password = new StringBuilder(12);
                // Ensure at least one of each type
                password.append(uppercase.charAt(RANDOM.nextInt(uppercase.length())));
                password.append(lowercase.charAt(RANDOM.nextInt(lowercase.length())));
                password.append(numbers.charAt(RANDOM.nextInt(numbers.length())));
                password.append(special.charAt(RANDOM.nextInt(special.length())));
                
                // Fill the rest randomly
                for (int i = 4; i < 12; i++) {
                        password.append(allChars.charAt(RANDOM.nextInt(allChars.length())));
                }
                
                // Shuffle the password
                char[] passwordArray = password.toString().toCharArray();
                for (int i = passwordArray.length - 1; i > 0; i--) {
                        int j = RANDOM.nextInt(i + 1);
                        char temp = passwordArray[i];
                        passwordArray[i] = passwordArray[j];
                        passwordArray[j] = temp;
                }
                
                return new String(passwordArray);
        }
}
