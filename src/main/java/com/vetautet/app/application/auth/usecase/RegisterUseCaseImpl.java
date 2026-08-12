package com.vetautet.app.application.auth.usecase;

import com.vetautet.app.application.auth.dto.RegisterCommand;
import com.vetautet.app.application.auth.dto.RegisterResultDto;
import com.vetautet.app.application.auth.port.input.RegisterUseCase;
import com.vetautet.app.application.auth.service.AuthFlowLinkService;
import com.vetautet.app.application.user.dto.CreateUserCommand;
import com.vetautet.app.application.user.dto.UserDto;
import com.vetautet.app.application.user.port.input.CreateUserUseCase;
import com.vetautet.app.domain.auth.model.AuthFlowType;
import com.vetautet.app.infrastructure.ratelimit.RateLimited;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
* Auth-facing registration use case.
* Delegates persistence and validation to the existing user creation flow.
*/
@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterUseCaseImpl implements RegisterUseCase {

    private final CreateUserUseCase createUserUseCase;
    private final AuthFlowLinkService authFlowLinkService;

    @Override
    @RateLimited(key = "#command.email.toLowerCase()", operation = "auth-register", permits = 3, periodSeconds = 3600)
    public RegisterResultDto execute(RegisterCommand command) {
        log.info("Executing RegisterUseCase for email: {}", command.getEmail());

        CreateUserCommand createUserCommand = CreateUserCommand.builder()
                .username(command.getUsername())
                .firstName(command.getFirstName())
                .lastName(command.getLastName())
                .email(command.getEmail())
                .password(command.getPassword())
                .mobileCountryCode(command.getMobileCountryCode())
                .mobileNumber(command.getMobileNumber())
                .avatarUrl(command.getAvatarUrl())
                .build();

        UserDto userDto = createUserUseCase.execute(createUserCommand);

        return RegisterResultDto.builder()
                .user(userDto)
                .activationLink(authFlowLinkService.issueLink(userDto.getUserId(), userDto.getEmail(),
                        AuthFlowType.REGISTER_ACTIVATION))
                .build();
    }
}