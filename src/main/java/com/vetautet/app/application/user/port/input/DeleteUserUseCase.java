

package com.vetautet.app.application.user.port.input;

/**
* Input port for deleting users
* Application layer - use case interface
*/
public interface DeleteUserUseCase {
    void execute(String userId);
}