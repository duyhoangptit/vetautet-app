package com.vetautet.app.infrastructure.config;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
* Configuration for internationalization (i18n)
* Supports multiple languages for error messages and application text
*/
@Configuration
public class I18nConfig {

    /**
     * Configure MessageSource for reading messages from properties files
     * Messages are loaded from messages.properties, messages_vi.properties, etc.
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setFallbackToSystemLocale(false); // Always fall back to messages.properties (Vietnamese default)
        messageSource.setCacheSeconds(3600); // Cache for 1 hour
        return messageSource;
    }

    /**
     * Configure LocaleResolver to determine the current locale
     * Uses Accept-Language header from HTTP request
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(Locale.forLanguageTag("vi")); // Default to Vietnamese
        return localeResolver;
    }
}
 