package com.vetautet.app.application.user.usecase;

import com.vetautet.app.application.auth.port.output.PasswordHashEncoder;
import com.vetautet.app.application.user.dto.CreateUserCommand;
import com.vetautet.app.application.user.dto.UserDto;
import com.vetautet.app.application.user.mapper.UserApplicationMapper;
import com.vetautet.app.application.user.port.input.CreateUserUseCase;
import com.vetautet.app.domain.user.exception.DuplicateEmailException;
import com.vetautet.app.domain.user.model.Email;
import com.vetautet.app.domain.user.model.User;
import com.vetautet.app.domain.user.repository.UserRepository;
import com.vetautet.app.domain.user.service.UserDomainService;
import com.vetautet.app.shared.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case implementation for creating users
 * Application layer - orchestrates domain logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreateUserUseCaseImpl implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final UserDomainService userDomainService;
    private final UserApplicationMapper mapper;
    private final PasswordHashEncoder passwordHashEncoder;

    @Override
    public UserDto execute(CreateUserCommand command) {
        log.info("Executing CreateUserUseCase for email: {}", command.getEmail());

        // Check for duplicate email (domain service)
        Email email = Email.of(command.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(ErrorCode.DUPLICATE_EMAIL, command.getEmail());
        }

        // Encode password before building domain object
        String passwordHash = passwordHashEncoder.encode(command.getPassword());

        // Convert command to domain model (with hashed password)
        User user = mapper.toDomain(command, passwordHash);

        // Validate domain rules
        user.validate();

        // Additional domain validation through domain service if needed
        userDomainService.validateNewUser(user);

        // Save user
        User savedUser = userRepository.save(user);

        log.info("Successfully created user with ID: {}", savedUser.getUserId().getValue());

        // Convert to DTO and return
        return mapper.toDto(savedUser);
    }
}
 