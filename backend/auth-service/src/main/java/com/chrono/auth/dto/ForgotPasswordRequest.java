package com.chrono.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor
public class ForgotPasswordRequest {

    @NotBlank
    @Email
    private String email;
}
