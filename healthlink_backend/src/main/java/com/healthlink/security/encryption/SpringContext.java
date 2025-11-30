package com.healthlink.security.encryption;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Simple holder to allow non-spring-managed classes to obtain Spring beans
 * (used by JPA AttributeConverters instantiated by the persistence provider).
 */
@Component
public class SpringContext implements ApplicationContextAware {

    private static ApplicationContext ctx;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        SpringContext.ctx = applicationContext;
    }

    public static <T> T getBean(Class<T> clazz) {
        if (ctx == null) {
            throw new IllegalStateException("Spring application context not initialized");
        }
        return ctx.getBean(clazz);
    }
}
