package com.chrono.auth.dto;

import com.chrono.commons.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserInfo user;

    @Getter
    @Builder
    public static class UserInfo {
        private String id;
        private String email;
        private String firstName;
        private String lastName;
        private UserRole role;
        private String tenantId;
    }

    public static String tokenType() {
        return "Bearer";
    }
}
