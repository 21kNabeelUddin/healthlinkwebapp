package com.healthlink.config;

import com.healthlink.domain.user.entity.*;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.appointment.entity.AppointmentStatus;
import com.healthlink.domain.appointment.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
@Profile({ "local", "dev" })
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedDatabase() {
        return args -> {
            if (userRepository.count() > 5) {
                log.info("Database already seeded, skipping");
                return;
            }

            log.info("Seeding database with test data...");

            // Create patients
            Patient patient1 = createPatient("john.doe@test.com", "John", "Doe", true);
            createPatient("jane.smith@test.com", "Jane", "Smith", false);

            // Create doctors
            Doctor doctor1 = createDoctor("dr.sarah@test.com", "Sarah", "Johnson", "Cardiology", true, true);
            createDoctor("dr.pending@test.com", "Mike", "Chen", "Neurology", false, false);

            // Create appointments
            Appointment apt1 = new Appointment();
            apt1.setPatient(patient1);
            apt1.setDoctor(doctor1);
            apt1.setAppointmentTime(LocalDateTime.now().plusDays(1));
            apt1.setEndTime(LocalDateTime.now().plusDays(1).plusMinutes(30));
            apt1.setStatus(AppointmentStatus.CONFIRMED);
            appointmentRepository.save(apt1);

            log.info("✅ Database seeded successfully!");
            log.info("Test users - Password for all: Test123!@#");
            log.info("  Patient (verified): john.doe@test.com");
            log.info("  Patient (unverified): jane.smith@test.com");
            log.info("  Doctor (approved): dr.sarah@test.com");
            log.info("  Doctor (pending): dr.pending@test.com");
        };
    }

    private Patient createPatient(String email, String firstName, String lastName, boolean verified) {
        if (userRepository.existsByEmail(email)) {
            return (Patient) userRepository.findByEmailAndDeletedAtIsNull(email).get();
        }
        Patient p = new Patient();
        p.setEmail(email);
        p.setPasswordHash(passwordEncoder.encode("Test123!@#"));
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setRole(UserRole.PATIENT);
        p.setApprovalStatus(ApprovalStatus.APPROVED);
        p.setIsEmailVerified(verified);
        p.setIsActive(true);
        p.setPreferredLanguage("en");
        return userRepository.save(p);
    }

    private Doctor createDoctor(String email, String firstName, String lastName, String spec, boolean approved,
            boolean verified) {
        if (userRepository.existsByEmail(email)) {
            return (Doctor) userRepository.findByEmailAndDeletedAtIsNull(email).get();
        }
        Doctor d = new Doctor();
        d.setEmail(email);
        d.setPasswordHash(passwordEncoder.encode("Test123!@#"));
        d.setFirstName(firstName);
        d.setLastName(lastName);
        d.setRole(UserRole.DOCTOR);
        d.setSpecialization(spec);
        // PMDC ID format: xxxxx-P (5 digits followed by -P)
        String pmdcSuffix = String.format("%05d", (int)(System.currentTimeMillis() % 100000));
        d.setPmdcId(pmdcSuffix + "-P");
        d.setPmdcVerified(approved);
        d.setApprovalStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING);
        d.setIsEmailVerified(verified);
        d.setIsActive(true);
        d.setPreferredLanguage("en");
        d.setConsultationFee(new BigDecimal("2000"));
        d.setSlotDurationMinutes(30);
        return userRepository.save(d);
    }
}
