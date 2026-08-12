package com.vetautet.app.application.auth.dto;


import com.vetautet.app.application.user.dto.UserDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResultDto {
    private UserDto user;
    private AuthFlowLinkDto activationLink;
}
