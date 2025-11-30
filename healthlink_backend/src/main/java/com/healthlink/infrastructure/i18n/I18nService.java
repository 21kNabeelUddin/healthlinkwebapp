package com.healthlink.infrastructure.i18n;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Internationalization service for retrieving localized messages.
 * Supports English and Urdu.
 * 
 * Usage:
 * - i18nService.getMessage("auth.login.success")
 * - i18nService.getMessage("validation.required", "Email")
 */
@Service
@RequiredArgsConstructor
public class I18nService {

    private final MessageSource messageSource;

    /**
     * Get localized message for current request locale.
     */
    public String getMessage(String key) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, null, key, locale);
    }

    /**
     * Get localized message with parameters for current request locale.
     */
    public String getMessage(String key, Object... params) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, params, key, locale);
    }

    /**
     * Get localized message for specific locale.
     */
    public String getMessage(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale);
    }

    /**
     * Get localized message with parameters for specific locale.
     */
    public String getMessage(String key, Locale locale, Object... params) {
        return messageSource.getMessage(key, params, key, locale);
    }

    /**
     * Get message in English (fallback).
     */
    public String getMessageInEnglish(String key, Object... params) {
        return messageSource.getMessage(key, params, key, Locale.ENGLISH);
    }

    /**
     * Get message in Urdu.
     */
    public String getMessageInUrdu(String key, Object... params) {
        return messageSource.getMessage(key, params, key, Locale.forLanguageTag("ur"));
    }
}
