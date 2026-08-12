package com.vetautet.app.shared.common.util;

import com.vetautet.app.shared.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Utility service for resolving internationalized messages
 * Uses Spring's MessageSource to fetch messages from properties files
 */
@Component
@RequiredArgsConstructor
public class MessageResolver {

    private final MessageSource messageSource;

    /**
     * Get message for the given error code using current locale
     *
     * @param errorCode the error code
     * @return localized message
     */
    public String getMessage(ErrorCode errorCode) {
        return getMessage(errorCode.getCode());
    }

    /**
     * Get message for the given error code with arguments using current locale
     *
     * @param errorCode the error code
     * @param args      message arguments for placeholder substitution
     * @return localized message with arguments
     */
    public String getMessage(ErrorCode errorCode, Object... args) {
        return getMessage(errorCode.getCode(), args);
    }

    /**
     * Get message for the given message key using current locale
     *
     * @param key the message key
     * @return localized message
     */
    public String getMessage(String key) {
        return getMessage(key, null, LocaleContextHolder.getLocale());
    }

    /**
     * Get message for the given message key with arguments using current locale
     *
     * @param key  the message key
     * @param args message arguments for placeholder substitution
     * @return localized message with arguments
     */
    public String getMessage(String key, Object... args) {
        return getMessage(key, args, LocaleContextHolder.getLocale());
    }

    /**
     * Get message for the given key with specific locale
     *
     * @param key    the message key
     * @param args   message arguments
     * @param locale the locale
     * @return localized message
     */
    public String getMessage(String key, Object[] args, Locale locale) {
        return messageSource.getMessage(key, args, locale);
    }

    /**
     * Get message for the given key with default message fallback
     *
     * @param key            the message key
     * @param defaultMessage default message if key not found
     * @return localized message or default message
     */
    public String getMessageOrDefault(String key, String defaultMessage) {
        return messageSource.getMessage(key, null, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * Get message for the given error code with default message fallback
     *
     * @param errorCode      the error code
     * @param defaultMessage default message if key not found
     * @param args           message arguments
     * @return localized message or default message
     */
    public String getMessageOrDefault(ErrorCode errorCode, String defaultMessage, Object... args) {
        return messageSource.getMessage(errorCode.getCode(), args, defaultMessage, LocaleContextHolder.getLocale());
    }
}
