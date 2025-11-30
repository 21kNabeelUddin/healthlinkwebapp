package com.healthlink.infrastructure.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class SafeLoggerTest {

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(SafeLoggerTest.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    void audit_shouldLogSanitizedMessage() {
        SafeLogger safeLogger = SafeLogger.get(SafeLoggerTest.class);
        
        String actor = "doctor_john";
        String action = "VIEW_RECORD";
        String resource = "Record-123";
        String reason = "Checking patient@email.com details for 555-123-4567";

        safeLogger.audit(actor, action, resource, reason);

        assertEquals(1, listAppender.list.size(), "Should log exactly one event");
        ILoggingEvent event = listAppender.list.get(0);
        
        assertEquals(Level.INFO, event.getLevel());
        String message = event.getFormattedMessage();
        
        // Verify structure
        assertTrue(message.contains("AUDIT"), "Log should contain event type");
        assertTrue(message.contains("Actor: doctor_john"));
        assertTrue(message.contains("Action: VIEW_RECORD"));
        
        // Verify sanitization
        assertFalse(message.contains("patient@email.com"), "PHI (email) should be scrubbed from reason");
        assertFalse(message.contains("555-123-4567"), "PHI (phone) should be scrubbed from reason");
    }
}
