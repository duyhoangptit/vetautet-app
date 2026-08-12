package com.vetautet.app.application.user.usecase;

import com.vetautet.app.application.user.dto.UpdateUserCommand;
import com.vetautet.app.application.user.dto.UserDto;
import com.vetautet.app.application.user.mapper.UserApplicationMapper;
import com.vetautet.app.application.user.port.input.UpdateUserUseCase;
import com.vetautet.app.domain.user.exception.DuplicateEmailException;
import com.vetautet.app.domain.user.model.Email;
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
 * Use case implementation for updating users
 * Application layer - orchestrates domain logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UpdateUserUseCaseImpl implements UpdateUserUseCase {

    private final UserRepository userRepository;
    private final UserApplicationMapper mapper;

    @Override
    public UserDto execute(UpdateUserCommand command) {
        log.info("Executing UpdateUserUseCase for user ID: {}", command.getUserId());

        // Find existing user
        UserId userId = UserId.of(command.getUserId());
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, command.getUserId()));

        // Check for duplicate email if email is being changed
        if (command.getEmail() != null && !command.getEmail().equals(existingUser.getEmail().getValue())) {
            Email newEmail = Email.of(command.getEmail());
            if (userRepository.existsByEmail(newEmail)) {
                throw new DuplicateEmailException(ErrorCode.DUPLICATE_EMAIL, command.getEmail());
            }
        }

        // Update domain model
        User updatedUser = mapper.toDomain(command, existingUser);

        // Validate domain rules
        updatedUser.validate();

        // Save updated user
        User savedUser = userRepository.save(updatedUser);

        log.info("Successfully updated user with ID: {}", savedUser.getUserId().getValue());

        // Convert to DTO and return
        return mapper.toDto(savedUser);
    }
}
 