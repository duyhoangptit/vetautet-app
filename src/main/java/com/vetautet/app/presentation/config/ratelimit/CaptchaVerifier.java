package com.vetautet.app.presentation.config.ratelimit;

/**
 * Port for captcha providers (e.g. reCAPTCHA v3, Cloudflare Turnstile).
 * Until a real provider is wired in, implementations must fail closed so a
 * challenged IP cannot bypass the challenge by sending an arbitrary header.
 */
public interface CaptchaVerifier {

    boolean verify(String token, String clientIp);
}
