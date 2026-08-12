package com.vetautet.app.infrastructure.auth;

import com.vetautet.app.application.auth.port.output.PasswordHashEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
* BCrypt implementation of the PasswordHashEncoder output port.
* Infrastructure layer - keeps Spring Security dependency isolated here.
*/
@Component
public class PasswordHashEncoderAdapter implements PasswordHashEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return bcrypt.matches(rawPassword, encodedPassword);
    }
}
 