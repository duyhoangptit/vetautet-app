package com.vetautet.app.application.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Command for user registration.
 * Application layer - use case input.
 */
@Data
@Builder
public class RegisterCommand {
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String mobileCountryCode;
    private String mobileNumber;
    private String avatarUrl;

}

