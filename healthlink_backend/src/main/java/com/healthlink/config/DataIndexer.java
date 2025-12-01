package com.healthlink.config;

import com.healthlink.domain.search.service.DoctorSearchService;
import com.healthlink.domain.user.entity.Doctor;
import com.healthlink.domain.user.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // Don't run in tests to avoid interference
@ConditionalOnProperty(prefix = "spring.data.elasticsearch.repositories", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DataIndexer implements CommandLineRunner {

    private final DoctorRepository doctorRepository;
    private final DoctorSearchService doctorSearchService;

    @Override
    @Transactional(readOnly = true)
    public void run(String... args) throws Exception {
        log.info("Starting data indexing...");
        try {
            List<Doctor> doctors = doctorRepository.findAll();
            log.info("Found {} doctors to index.", doctors.size());
            for (Doctor doctor : doctors) {
                try {
                    doctorSearchService.indexDoctor(doctor);
                } catch (Exception e) {
                    log.error("Failed to index doctor {}: {}", doctor.getId(), e.getMessage());
                }
            }
            log.info("Data indexing completed.");
        } catch (Exception e) {
            log.error("Data indexing failed", e);
        }
    }
}
