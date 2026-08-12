
package com.vetautet.app.application.user.dto;

import lombok.Builder;
import lombok.Data;

/**
* Command object for updating a user
* Application layer - use case input
*/
@Data
@Builder
public class UpdateUserCommand {
    private String userId;
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
}