package com.healthlink.security.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PhiAccess {
    String reason();
    Class<?> entityType();
    String idParam() default "id"; // name of method parameter holding the entity id
}
