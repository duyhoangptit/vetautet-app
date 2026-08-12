package com.vetautet.app.application.auth.dto;

import lombok.Builder;
import lombok.Data;


/**
 * Command for user login
 * Application layer - use case input
 */
@Data
@Builder
public class LoginCommand {
    private String email;
    private String password;
    private String portal;
}

