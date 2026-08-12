
package com.vetautet.app.application.user.port.input;

import com.vetautet.app.application.user.dto.UpdateUserCommand;
import com.vetautet.app.application.user.dto.UserDto;

/**
* Input port for updating users
* Application layer - use case interface
*/
public interface UpdateUserUseCase {
    UserDto execute(UpdateUserCommand command);
}
 