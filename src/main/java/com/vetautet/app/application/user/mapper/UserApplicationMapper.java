package com.vetautet.app.application.user.mapper;

import com.vetautet.app.application.user.dto.CreateUserCommand;
import com.vetautet.app.application.user.dto.UpdateUserCommand;
import com.vetautet.app.application.user.dto.UserDto;
import com.vetautet.app.domain.user.model.*;
import org.springframework.stereotype.Component;

/**
* Mapper between domain models and application DTOs
* Application layer
*/
@Component
public class UserApplicationMapper {

    public User toDomain(CreateUserCommand command) {
        return toDomain(command, null);
    }

    public User toDomain(CreateUserCommand command, String passwordHash) {
        if (command == null) {
            return null;
        }

        PersonalInfo.Builder personalInfoBuilder = PersonalInfo.builder()
                .firstName(command.getFirstName())
                .lastName(command.getLastName());

        if (command.getMobileCountryCode() != null) {
            personalInfoBuilder.mobileCountryCode(command.getMobileCountryCode());
        }
        if (command.getMobileNumber() != null) {
            personalInfoBuilder.mobileNumber(command.getMobileNumber());
        }
        if (command.getAvatarUrl() != null) {
            personalInfoBuilder.avatarUrl(command.getAvatarUrl());
        }

        User.UserBuilder userBuilder = User.builder()
                .personalInfo(personalInfoBuilder.build())
                .email(Email.of(command.getEmail()))
                .status(UserStatus.PENDING) // Default status for new users
                .isOnboarding(true) // Default onboarding status
                .passwordHash(passwordHash);

        if (command.getUsername() != null) {
            userBuilder.username(command.getUsername());
        }

        return userBuilder.build();
    }

    public User toDomain(UpdateUserCommand command, User existingUser) {
        if (command == null || existingUser == null) {
            return null;
        }

        User.UserBuilder builder = existingUser.toBuilder();

        if (command.getUsername() != null) {
            builder.username(command.getUsername());
        }

        // Update personal info
        PersonalInfo.Builder personalInfoBuilder = PersonalInfo.builder()
                .firstName(command.getFirstName() != null ? command.getFirstName()
                        : existingUser.getPersonalInfo().getFirstName())
                .lastName(command.getLastName() != null ? command.getLastName()
                        : existingUser.getPersonalInfo().getLastName());

        if (command.getMobileCountryCode() != null) {
            personalInfoBuilder.mobileCountryCode(command.getMobileCountryCode());
        } else if (existingUser.getPersonalInfo().getMobileCountryCode() != null) {
            personalInfoBuilder.mobileCountryCode(existingUser.getPersonalInfo().getMobileCountryCode());
        }

        if (command.getMobileNumber() != null) {
            personalInfoBuilder.mobileNumber(command.getMobileNumber());
        } else if (existingUser.getPersonalInfo().getMobileNumber() != null) {
            personalInfoBuilder.mobileNumber(existingUser.getPersonalInfo().getMobileNumber());
        }

        if (command.getAvatarUrl() != null) {
            personalInfoBuilder.avatarUrl(command.getAvatarUrl());
        } else if (existingUser.getPersonalInfo().getAvatarUrl() != null) {
            personalInfoBuilder.avatarUrl(existingUser.getPersonalInfo().getAvatarUrl());
        }

        builder.personalInfo(personalInfoBuilder.build());

        if (command.getEmail() != null) {
            builder.email(Email.of(command.getEmail()));
        }

        if (command.getStatus() != null) {
            builder.status(UserStatus.fromCode(command.getStatus()));
        }

        if (command.getIsOnboarding() != null) {
            builder.isOnboarding(command.getIsOnboarding());
        }

        return builder.build();
    }

    public UserDto toDto(User domain) {
        if (domain == null) {
            return null;
        }

        UserDto.UserDtoBuilder builder = UserDto.builder()
                .userId(domain.getUserId() != null ? domain.getUserId().getValue() : null)
                .username(domain.getUsername())
                .firstName(domain.getPersonalInfo().getFirstName())
                .lastName(domain.getPersonalInfo().getLastName())
                .email(domain.getEmail().getValue())
                .mobileCountryCode(domain.getPersonalInfo().getMobileCountryCode())
                .mobileNumber(domain.getPersonalInfo().getMobileNumber())
                .avatarUrl(domain.getPersonalInfo().getAvatarUrl())
                .status(domain.getStatus().getCode())
                .isOnboarding(domain.getIsOnboarding())
                .createdDate(domain.getCreatedDate())
                .lastModifiedDate(domain.getLastModifiedDate())
                .createdBy(domain.getCreatedBy())
                .lastModifiedBy(domain.getLastModifiedBy());

        return builder.build();
    }
}
 