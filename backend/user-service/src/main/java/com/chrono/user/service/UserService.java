package com.chrono.user.service;

import com.chrono.commons.constants.ApiConstants;
import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.enums.UserRole;
import com.chrono.commons.exception.BusinessException;
import com.chrono.commons.exception.DuplicateResourceException;
import com.chrono.commons.exception.ResourceNotFoundException;
import com.chrono.commons.service.BaseService;
import com.chrono.user.client.AuthServiceClient;
import com.chrono.user.client.dto.RegisterCredentialRequest;
import com.chrono.user.domain.entity.UserProfile;
import com.chrono.user.domain.entity.UserStatus;
import com.chrono.user.domain.repository.UserProfileRepository;
import com.chrono.user.dto.CreateUserRequest;
import com.chrono.user.dto.UpdateUserRequest;
import com.chrono.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends BaseService {

    private final UserProfileRepository userProfileRepository;
    private final AuthServiceClient authServiceClient;

    @Transactional
    public UserProfileResponse createUser(
            CreateUserRequest request, String callerTenantId, String callerRole) {

        // Only PLATFORM_ADMIN can skip tenant isolation check for cross-tenant ops
        String targetTenantId = callerTenantId;

        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("UserProfile", "email", request.getEmail());
        }

        // TENANT_USER cannot create other admins
        if (UserRole.TENANT_USER.name().equals(callerRole)
                && request.getRole() != UserRole.TENANT_USER) {
            throw new BusinessException("Insufficient permissions to assign role: " + request.getRole());
        }

        String userId = UUID.randomUUID().toString();

        // 1. Create profile
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setEmail(request.getEmail());
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhone(request.getPhone());
        profile.setTenantId(targetTenantId);
        profile.setRole(request.getRole());
        profile.setTimezone(request.getTimezone() != null ? request.getTimezone() : "UTC");

        userProfileRepository.save(profile);

        // 2. Register credentials via Feign (auth-service)
        authServiceClient.registerCredential(
                RegisterCredentialRequest.builder()
                        .userId(userId)
                        .email(request.getEmail())
                        .password(request.getPassword())
                        .role(request.getRole())
                        .tenantId(targetTenantId)
                        .build());

        log.info("User created: userId={}, tenantId={}, role={}", userId, targetTenantId, request.getRole());
        return UserProfileResponse.from(profile);
    }

    @Cacheable(value = "userProfiles", key = "#userId")
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(String userId, String callerTenantId, String callerRole) {
        UserProfile profile = findActiveProfile(userId);
        assertTenantAccess(profile.getTenantId(), callerTenantId, callerRole);
        return UserProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> listUsersByTenant(
            String tenantId, String search, UserStatus status,
            PageRequest pageRequest, String callerTenantId, String callerRole) {

        assertTenantAccess(tenantId, callerTenantId, callerRole);

        org.springframework.data.domain.Page<UserProfile> page =
                userProfileRepository.searchByTenant(
                        tenantId, search, status, pageRequest.toSpringPageRequest());

        return PageResponse.from(page.map(UserProfileResponse::from));
    }

    @CacheEvict(value = "userProfiles", key = "#userId")
    @Transactional
    public UserProfileResponse updateUser(
            String userId, UpdateUserRequest request, String callerTenantId, String callerRole) {

        UserProfile profile = findActiveProfile(userId);
        assertTenantAccess(profile.getTenantId(), callerTenantId, callerRole);

        if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null)  profile.setLastName(request.getLastName());
        if (request.getPhone() != null)     profile.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) profile.setAvatarUrl(request.getAvatarUrl());
        if (request.getTimezone() != null)  profile.setTimezone(request.getTimezone());

        log.info("User updated: userId={}", userId);
        return UserProfileResponse.from(profile);
    }

    @CacheEvict(value = "userProfiles", key = "#userId")
    @Transactional
    public void deleteUser(String userId, String callerTenantId, String callerRole) {
        UserProfile profile = findActiveProfile(userId);
        assertTenantAccess(profile.getTenantId(), callerTenantId, callerRole);

        profile.setDeletedAt(LocalDateTime.now());
        log.info("User soft-deleted: userId={}", userId);
    }

    private UserProfile findActiveProfile(String userId) {
        return userProfileRepository.findByUserId(userId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
