package com.vetautet.app.presentation.rest.controller.v1;

import com.vetautet.app.application.user.dto.UserDto;
import com.vetautet.app.application.user.port.input.CreateUserUseCase;
import com.vetautet.app.application.user.port.input.DeleteUserUseCase;
import com.vetautet.app.application.user.port.input.GetUserUseCase;
import com.vetautet.app.application.user.port.input.UpdateUserUseCase;
import com.vetautet.app.infrastructure.idempotency.Idempotent;
import com.vetautet.app.presentation.config.RequireBearerAuth;
import com.vetautet.app.presentation.rest.dto.request.CreateUserRequest;
import com.vetautet.app.presentation.rest.dto.request.UpdateUserRequest;
import com.vetautet.app.presentation.rest.dto.response.BaseResponse;
import com.vetautet.app.presentation.rest.dto.response.PageResponse;
import com.vetautet.app.presentation.rest.dto.response.UserResponse;
import com.vetautet.app.presentation.rest.mapper.UserPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
* REST Controller for User management
* Presentation layer - handles HTTP requests
* Uses Clean Architecture use cases
*/
@RestController
@RequestMapping(value = "/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for managing users")
@RequireBearerAuth
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    /**
     * Hard upper bound on client-supplied page size. Without this, a caller
     * can request an arbitrarily large {@code size} and force the query to
     * load the entire table in one page (unbounded resource consumption /
     * OWASP API4:2023). Values above this are silently clamped rather than
     * rejected, matching common pagination API conventions (e.g. GitHub).
     */
    private static final int MAX_PAGE_SIZE = 100;

    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UserPresentationMapper mapper;

    @PostMapping
    @Idempotent(key = "#request.email.toLowerCase()", operation = "create-user", ttlHours = 24, hashFields = {"#request"})
    @Operation(summary = "Create a new user", description = "Creates a new user in the system")
    public ResponseEntity<BaseResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {
        log.info("Creating new user with email: {}", request.getEmail());

        UserDto userDto = createUserUseCase.execute(mapper.toCommand(request));
        UserResponse response = mapper.toResponse(userDto);
        BaseResponse<UserResponse> baseResponse = BaseResponse.success(
                HttpStatus.CREATED.value(),
                "User created successfully",
                response,
                httpRequest.getRequestURI());

        log.info("Successfully created user with ID: {}", response.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(baseResponse);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user", description = "Updates an existing user by ID")
    public ResponseEntity<BaseResponse<UserResponse>> updateUser(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest) {
        log.info("Updating user with ID: {}", userId);

        UserDto userDto = updateUserUseCase.execute(mapper.toCommand(userId.toString(), request));
        UserResponse response = mapper.toResponse(userDto);
        BaseResponse<UserResponse> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(),
                "User updated successfully",
                response,
                httpRequest.getRequestURI());

        log.info("Successfully updated user with ID: {}", userId);
        return ResponseEntity.ok(baseResponse);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user by their unique ID")
    public ResponseEntity<BaseResponse<UserResponse>> getUserById(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            HttpServletRequest httpRequest) {
        log.info("Fetching user with ID: {}", userId);

        UserDto userDto = getUserUseCase.findById(userId.toString());
        UserResponse response = mapper.toResponse(userDto);
        BaseResponse<UserResponse> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(),
                "User retrieved successfully",
                response,
                httpRequest.getRequestURI());

        return ResponseEntity.ok(baseResponse);
    }

    @GetMapping
    @Operation(summary = "List all users", description = "Retrieves a paginated list of all users")
    public ResponseEntity<BaseResponse<PageResponse<UserResponse>>> listUsers(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (capped at " + MAX_PAGE_SIZE + ")") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdDate") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDir,
            HttpServletRequest httpRequest) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        log.info("Fetching users - page: {}, size: {}, sortBy: {}, sortDir: {}", page, safeSize, sortBy, sortDir);

        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(direction, sortBy));

        Page<UserDto> userPage = getUserUseCase.findAll(pageable);
        PageResponse<UserResponse> response = mapper.toPageResponse(userPage);
        BaseResponse<PageResponse<UserResponse>> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(),
                "Users retrieved successfully",
                response,
                httpRequest.getRequestURI());

        log.info("Successfully fetched {} users", response.getContent().size());
        return ResponseEntity.ok(baseResponse);
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users by keyword in name or email")
    public ResponseEntity<BaseResponse<PageResponse<UserResponse>>> searchUsers(
            @Parameter(description = "Search keyword") @RequestParam String keyword,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (capped at " + MAX_PAGE_SIZE + ")") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        log.info("Searching users with keyword: {}", keyword);

        Pageable pageable = PageRequest.of(page, safeSize);
        Page<UserDto> userPage = getUserUseCase.searchByKeyword(keyword, pageable);
        PageResponse<UserResponse> response = mapper.toPageResponse(userPage);
        BaseResponse<PageResponse<UserResponse>> baseResponse = BaseResponse.success(
                HttpStatus.OK.value(),
                "Users searched successfully",
                response,
                httpRequest.getRequestURI());

        log.info("Found {} users matching keyword: {}", response.getTotalElements(), keyword);
        return ResponseEntity.ok(baseResponse);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Deletes a user by their ID")
    public ResponseEntity<BaseResponse<Void>> deleteUser(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            HttpServletRequest httpRequest) {
        log.info("Deleting user with ID: {}", userId);

        deleteUserUseCase.execute(userId.toString());
        BaseResponse<Void> baseResponse = BaseResponse.success(
                HttpStatus.NO_CONTENT.value(),
                "User deleted successfully",
                httpRequest.getRequestURI());

        log.info("Successfully deleted user with ID: {}", userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(baseResponse);
    }
}
 