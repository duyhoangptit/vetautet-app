package com.vetautet.app.application.auth.port.input;

/**
 * Input port for logout use case
 * Application layer - use case interface
 */
public interface LogoutUseCase {
    void execute(String userId, String keyId);
}