
package com.vetautet.app.presentation.rest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
* Response DTO for user data
* Presentation layer - API contract
*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileCountryCode;
    private String mobileNumber;
    private String avatarUrl;
    private String memberPolicyNumber;
    private String memberCompanyId;
    private String memberNumber;
    private String dependentNumber;
    private String status;
    private Boolean isOnboarding;
    private Instant createdDate;
    private Instant lastModifiedDate;
    private String createdBy;
    private String lastModifiedBy;
}