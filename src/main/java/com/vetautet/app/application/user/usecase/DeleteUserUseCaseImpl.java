package com.vetautet.app.application.user.usecase;

import com.vetautet.app.application.user.port.input.DeleteUserUseCase;
import com.vetautet.app.domain.user.model.User;
import com.vetautet.app.domain.user.model.UserId;
import com.vetautet.app.domain.user.repository.UserRepository;
import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case implementation for deleting users
 * Application layer - orchestrates domain logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeleteUserUseCaseImpl implements DeleteUserUseCase {

    private final UserRepository userRepository;

    @Override
    public void execute(String userIdString) {
        log.info("Executing DeleteUserUseCase for user ID: {}", userIdString);

        UserId userId = UserId.of(userIdString);

        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, userIdString));

        // Check if user can be deleted (business rule)
        if (!user.canBeDeleted()) {
            throw new IllegalStateException("User with status " + user.getStatus() + " cannot be deleted");
        }

        // Delete user
        userRepository.delete(userId);

        log.info("Successfully deleted user with ID: {}", userIdString);
    }
}
 