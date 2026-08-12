package com.vetautet.app.presentation.config.ratelimit;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Default {@link CaptchaVerifier} bean used until a real captcha provider is
 * configured. Always rejects so challenged IPs cannot bypass the challenge
 * by sending an arbitrary token - intentional fail-closed behavior.
 */
@Slf4j
@Component
public class NoOpCaptchaVerifier implements CaptchaVerifier {

    @Override
    public boolean verify(String token, String clientIp) {
        log.warn("Captcha verification requested without a configured provider, clientIp={}", clientIp);
        return false;
    }
}
