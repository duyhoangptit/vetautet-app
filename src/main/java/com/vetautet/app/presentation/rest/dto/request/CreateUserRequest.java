
package com.vetautet.app.presentation.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
* Request DTO for creating a new user
* Presentation layer - API contract
*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @Size(max = 100, message = "Username cannot exceed 100 characters")
    private String username;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @Size(max = 10, message = "Mobile country code cannot exceed 10 characters")
    private String mobileCountryCode;

    @Size(max = 20, message = "Mobile number cannot exceed 20 characters")
    private String mobileNumber;

    @Size(max = 100, message = "Avatar URL cannot exceed 100 characters")
    private String avatarUrl;

    @Size(max = 8, message = "Member policy number cannot exceed 8 characters")
    private String memberPolicyNumber;

    @Size(max = 8, message = "Member company ID cannot exceed 8 characters")
    private String memberCompanyId;

    @Size(max = 5, message = "Member number cannot exceed 5 characters")
    private String memberNumber;

    @Size(max = 2, message = "Dependent number cannot exceed 2 characters")
    private String dependentNumber;
}