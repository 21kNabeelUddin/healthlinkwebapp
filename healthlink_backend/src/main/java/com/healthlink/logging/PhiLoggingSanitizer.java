package com.healthlink.logging;

import java.util.regex.Pattern;

/**
 * Utility to scrub potential PHI fragments from audit metadata strings before persistence.
 * Conservative approach: remove email-like, phone-like, and long numeric sequences.
 */
public final class PhiLoggingSanitizer {
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(?:(?:\\+?\\d{1,3})?[-. (]*\\d{3}[-. )]*\\d{3}[-. ]*\\d{4})");
    private static final Pattern LONG_NUMBER = Pattern.compile("\\b\\d{8,}\\b");

    private PhiLoggingSanitizer() {}

    public static String sanitizeReason(String reason) {
        if (reason == null) return null;
        String r = reason;
        r = EMAIL.matcher(r).replaceAll("[EMAIL]");
        r = PHONE.matcher(r).replaceAll("[PHONE]");
        r = LONG_NUMBER.matcher(r).replaceAll("[NUM]");
        return truncate(r);
    }

    public static String sanitizeIdentifier(String id) {
        if (id == null) return "unknown";
        // identifiers should generally be UUIDs; if not, scrub
        String v = EMAIL.matcher(id).replaceAll("[EMAIL]");
        v = PHONE.matcher(v).replaceAll("[PHONE]");
        v = LONG_NUMBER.matcher(v).replaceAll("[NUM]");
        return truncate(v);
    }

    private static String truncate(String s) {
        return s.length() > 200 ? s.substring(0, 200) : s;
    }
}
