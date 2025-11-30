package com.healthlink.security.encryption;

import com.healthlink.AbstractIntegrationTest;
import com.healthlink.domain.user.entity.Patient;
import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.enums.UserRole;
import com.healthlink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
        "healthlink.phi.encryption-key=12345678901234567890123456789012EXTRA",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.main.allow-bean-definition-overriding=true"
})
@org.springframework.test.annotation.DirtiesContext
class UserPhiEncryptionMigrationRunnerTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPhiEncryptionMigrationRunner runner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migratesPlaintextFields() {
        // Use concrete Patient entity instead of anonymous inner class for Hibernate
        Patient patient = new Patient();
        patient.setEmail("test@example.com");
        patient.setFirstName("Alice");
        patient.setLastName("Smith");
        patient.setPhoneNumber("+123456789");
        patient.setRole(UserRole.PATIENT);
        patient.setIsActive(true);
        patient.setIsEmailVerified(true);
        userRepository.save(patient);

        runner.run(null); // perform migration

        // Verify the data is encrypted in the database (raw SQL bypasses the converter)
        String dbFirstName = jdbcTemplate.queryForObject(
            "SELECT first_name FROM users WHERE email = ?", String.class, "test@example.com");
        String dbLastName = jdbcTemplate.queryForObject(
            "SELECT last_name FROM users WHERE email = ?", String.class, "test@example.com");
        String dbPhoneNumber = jdbcTemplate.queryForObject(
            "SELECT phone_number FROM users WHERE email = ?", String.class, "test@example.com");

        assertNotNull(dbFirstName);
        assertTrue(dbFirstName.startsWith("ENC:"), "Database first_name should be encrypted");
        assertTrue(dbLastName.startsWith("ENC:"), "Database last_name should be encrypted");
        assertTrue(dbPhoneNumber.startsWith("ENC:"), "Database phone_number should be encrypted");

        // Also verify that loading through JPA (with converter) gives back the original values
        User reloaded = userRepository.findByEmail("test@example.com").orElseThrow();
        assertEquals("Alice", reloaded.getFirstName());
        assertEquals("Smith", reloaded.getLastName());
        assertEquals("+123456789", reloaded.getPhoneNumber());
    }
}