package com.healthlink.security.audit;

import java.lang.annotation.*;

/**
 * Annotation to mark methods that access PHI. Triggers PhiAccessAspect for audit logging.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PhiAccess {
    String operation();
}