package com.vetautet.app.application.user.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserDto {
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
