package com.vetautet.app.infrastructure.auth;

import com.vetautet.app.application.auth.port.output.OtpCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Profile("!pentest")
@Component
public class RandomOtpCodeGeneratorAdapter implements OtpCodeGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public String generateSixDigitCode() {
        int value = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}