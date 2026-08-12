package com.vetautet.app.presentation.rest.mapper;

import com.vetautet.app.application.user.dto.CreateUserCommand;
import com.vetautet.app.application.user.dto.UpdateUserCommand;
import com.vetautet.app.application.user.dto.UserDto;
import com.vetautet.app.presentation.rest.dto.request.CreateUserRequest;
import com.vetautet.app.presentation.rest.dto.request.UpdateUserRequest;
import com.vetautet.app.presentation.rest.dto.response.PageResponse;
import com.vetautet.app.presentation.rest.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
* Mapper for presentation layer
* Converts between REST DTOs and Application DTOs
*/
@Component
public class UserPresentationMapper {

    /**
     * Convert CreateUserRequest to CreateUserCommand
     */
    public CreateUserCommand toCommand(CreateUserRequest request) {
        if (request == null) {
            return null;
        }

        return CreateUserCommand.builder()
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword())
                .mobileCountryCode(request.getMobileCountryCode())
                .mobileNumber(request.getMobileNumber())
                .avatarUrl(request.getAvatarUrl())
                .memberPolicyNumber(request.getMemberPolicyNumber())
                .memberCompanyId(request.getMemberCompanyId())
                .memberNumber(request.getMemberNumber())
                .dependentNumber(request.getDependentNumber())
                .build();
    }

    /**
     * Convert UpdateUserRequest to UpdateUserCommand
     */
    public UpdateUserCommand toCommand(String userId, UpdateUserRequest request) {
        if (request == null) {
            return null;
        }

        return UpdateUserCommand.builder()
                .userId(userId)
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .mobileCountryCode(request.getMobileCountryCode())
                .mobileNumber(request.getMobileNumber())
                .avatarUrl(request.getAvatarUrl())
                .memberPolicyNumber(request.getMemberPolicyNumber())
                .memberCompanyId(request.getMemberCompanyId())
                .memberNumber(request.getMemberNumber())
                .dependentNumber(request.getDependentNumber())
                .status(request.getStatus())
                .isOnboarding(request.getIsOnboarding())
                .build();
    }

    /**
     * Convert UserDto to UserResponse
     */
    public UserResponse toResponse(UserDto dto) {
        if (dto == null) {
            return null;
        }

        return UserResponse.builder()
                .userId(dto.getUserId())
                .username(dto.getUsername())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .mobileCountryCode(dto.getMobileCountryCode())
                .mobileNumber(dto.getMobileNumber())
                .avatarUrl(dto.getAvatarUrl())
                .memberPolicyNumber(dto.getMemberPolicyNumber())
                .memberCompanyId(dto.getMemberCompanyId())
                .memberNumber(dto.getMemberNumber())
                .dependentNumber(dto.getDependentNumber())
                .status(dto.getStatus())
                .isOnboarding(dto.getIsOnboarding())
                .createdDate(dto.getCreatedDate())
                .lastModifiedDate(dto.getLastModifiedDate())
                .createdBy(dto.getCreatedBy())
                .lastModifiedBy(dto.getLastModifiedBy())
                .build();
    }

    /**
     * Convert Page<UserDto> to PageResponse<UserResponse>
     */
    public PageResponse<UserResponse> toPageResponse(Page<UserDto> page) {
        if (page == null) {
            return null;
        }

        return PageResponse.<UserResponse>builder()
                .content(page.getContent().stream()
                        .map(this::toResponse)
                        .toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}
 