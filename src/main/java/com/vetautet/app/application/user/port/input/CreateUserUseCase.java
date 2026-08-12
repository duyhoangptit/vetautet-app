package com.vetautet.app.application.user.port.input;

import com.vetautet.app.application.user.dto.CreateUserCommand;
import com.vetautet.app.application.user.dto.UserDto;

/**
* Input port for creating users
* Application layer - use case interface
*/
public interface CreateUserUseCase {
    UserDto execute(CreateUserCommand command);
}



