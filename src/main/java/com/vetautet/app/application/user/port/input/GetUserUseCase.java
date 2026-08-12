
package com.vetautet.app.application.user.port.input;

import com.vetautet.app.application.user.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
* Input port for querying users
* Application layer - use case interface
*/
public interface GetUserUseCase {
    UserDto findById(String userId);

    Page<UserDto> findAll(Pageable pageable);

    Page<UserDto> searchByKeyword(String keyword, Pageable pageable);
}
