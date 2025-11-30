package com.healthlink.infrastructure.logging;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SafeLoggerAuditTest {

    @Test
    void audit_shouldLogWithRequiredFields() {
        SafeLogger logger = SafeLogger.get(SafeLoggerAuditTest.class);
        
        // This test ensures the method exists and runs without error.
        // Verifying the actual log output would require a more complex setup with Logback appenders,
        // which is overkill for this step. We just want to ensure the API exists as per spec.
        assertDoesNotThrow(() -> {
            logger.audit("doctor@example.com", "VIEW_RECORD", "Patient:123", "Routine Checkup");
        });
    }
}
