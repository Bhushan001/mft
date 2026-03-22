package com.chrono.user.dto;

import com.chrono.commons.enums.UserRole;
import com.chrono.user.domain.entity.UserProfile;
import com.chrono.user.domain.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserProfileResponse {

    private Long id;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String tenantId;
    private UserRole role;
    private UserStatus status;
    private String avatarUrl;
    private String timezone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserProfileResponse from(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phone(profile.getPhone())
                .tenantId(profile.getTenantId())
                .role(profile.getRole())
                .status(profile.getStatus())
                .avatarUrl(profile.getAvatarUrl())
                .timezone(profile.getTimezone())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
