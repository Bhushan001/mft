package com.chrono.auth.dto;

import com.chrono.commons.enums.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Internal request from User Service to register credentials for a new user.
 * Not exposed directly to public; called via Feign from user-service.
 */
@Getter
@NoArgsConstructor
public class RegisterCredentialRequest {

    @NotBlank
    private String userId;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    private String password;

    @NotNull
    private UserRole role;

    @NotBlank
    private String tenantId;
}
