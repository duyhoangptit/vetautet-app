package com.vetautet.app.application.user.usecase;

import com.vetautet.app.application.user.dto.UserDto;
import com.vetautet.app.application.user.mapper.UserApplicationMapper;
import com.vetautet.app.application.user.port.input.GetUserUseCase;
import com.vetautet.app.domain.user.model.User;
import com.vetautet.app.domain.user.model.UserId;
import com.vetautet.app.domain.user.repository.UserRepository;
import com.vetautet.app.shared.common.exception.ErrorCode;
import com.vetautet.app.shared.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* Use case implementation for querying users
* Application layer - orchestrates domain logic
*/
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetUserUseCaseImpl implements GetUserUseCase {

    private final UserRepository userRepository;
    private final UserApplicationMapper mapper;

    @Override
    public UserDto findById(String userIdString) {
        log.info("Executing GetUserUseCase for user ID: {}", userIdString);

        UserId userId = UserId.of(userIdString);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, userIdString));

        return mapper.toDto(user);
    }

    @Override
    public Page<UserDto> findAll(Pageable pageable) {
        log.info("Executing GetUserUseCase to find all users - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(mapper::toDto);
    }

    @Override
    public Page<UserDto> searchByKeyword(String keyword, Pageable pageable) {
        log.info("Executing GetUserUseCase to search users by keyword: {}", keyword);

        Page<User> userPage = userRepository.searchByKeyword(keyword, pageable);
        return userPage.map(mapper::toDto);
    }
}
 