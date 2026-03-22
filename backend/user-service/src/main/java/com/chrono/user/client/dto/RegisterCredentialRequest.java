package com.chrono.user.client.dto;

import com.chrono.commons.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterCredentialRequest {
    private String userId;
    private String email;
    private String password;
    private UserRole role;
    private String tenantId;
}
