package com.healthlink.infrastructure.logging;

import com.healthlink.logging.PhiLoggingSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Secure logger that automatically masks PHI (Personal Health Information)
 * and PII (Personally Identifiable Information) before writing to logs.
 * 
 * Note: This is NOT a Spring bean. Use static factory methods to create instances.
 */
public class SafeLogger {

    private final Logger logger;
    
    // Patterns to mask
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+\\d{1,3}[- ]?)?\\d{10}");
    private static final Pattern CNIC_PATTERN = Pattern.compile("\\d{5}-\\d{7}-\\d{1}");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}");

    private SafeLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public static SafeLogger getLogger(Class<?> clazz) {
        return new SafeLogger(clazz);
    }

    /**
     * Get a SafeLogger instance for the given class
     */
    public static SafeLogger get(Class<?> clazz) {
        return new SafeLogger(clazz);
    }

    /**
     * Start building an event log entry with structured fields
     */
    public EventBuilder event(String eventName) {
        return new EventBuilder(this, eventName);
    }

    public void info(String message, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(mask(message), maskArgs(args));
        }
    }

    public void debug(String message, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(mask(message), maskArgs(args));
        }
    }

    public void error(String message, Throwable t) {
        if (logger.isErrorEnabled()) {
            logger.error(mask(message), t);
        }
    }

    public void error(String message, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(mask(message), maskArgs(args));
        }
    }

    public void warn(String message, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(mask(message), maskArgs(args));
        }
    }

    /**
     * Audit log for security events. These are structured and persisted if configured.
     * @param actor The user/service performing the action (masked if PHI)
     * @param action The action performed (e.g., "VIEW_RECORD")
     * @param resource The resource accessed (e.g., "Patient:123")
     * @param reason The reason for access (e.g., "Routine Checkup")
     */
    public void audit(String actor, String action, String resource, String reason) {
        logger.info("AUDIT | Actor: {} | Action: {} | Resource: {} | Reason: {}", 
            PhiLoggingSanitizer.sanitizeIdentifier(actor), 
            action, 
            PhiLoggingSanitizer.sanitizeIdentifier(resource), 
            PhiLoggingSanitizer.sanitizeReason(reason));
    }

    private String mask(String input) {
        if (input == null) return null;
        
        String masked = input;
        masked = maskPattern(masked, EMAIL_PATTERN);
        masked = maskPattern(masked, PHONE_PATTERN);
        masked = maskPattern(masked, CNIC_PATTERN);
        masked = maskPattern(masked, CREDIT_CARD_PATTERN);
        
        return masked;
    }

    private String maskPattern(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "***REDACTED***");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Object[] maskArgs(Object[] args) {
        if (args == null) return null;
        Object[] maskedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String) {
                maskedArgs[i] = mask((String) args[i]);
            } else {
                maskedArgs[i] = args[i];
            }
        }
        return maskedArgs;
    }

    /**
     * Builder for structured event logging
     */
    public static class EventBuilder {
        private final SafeLogger logger;
        private final String eventName;
        private final StringBuilder fields = new StringBuilder();

        private EventBuilder(SafeLogger logger, String eventName) {
            this.logger = logger;
            this.eventName = eventName;
        }

        public EventBuilder withMasked(String key, String value) {
            fields.append(" | ").append(key).append(": ").append(logger.mask(value));
            return this;
        }

        public EventBuilder with(String key, Object value) {
            fields.append(" | ").append(key).append(": ").append(value);
            return this;
        }

        public void log() {
            logger.info("EVENT: {}{}", eventName, fields.toString());
        }
    }
}
