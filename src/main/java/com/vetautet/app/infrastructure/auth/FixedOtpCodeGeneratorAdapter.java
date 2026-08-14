package com.vetautet.app.infrastructure.auth;

import com.vetautet.app.application.auth.port.output.OtpCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("pentest")
@Component
public class FixedOtpCodeGeneratorAdapter implements OtpCodeGenerator {

    private static final String FIX_OTP_VALUE = "123456";

    @Override
    public String generateSixDigitCode() {
        return FIX_OTP_VALUE;
    }
}