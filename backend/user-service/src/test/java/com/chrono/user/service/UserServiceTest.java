package com.chrono.user.service;

import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.enums.UserRole;
import com.chrono.commons.exception.BusinessException;
import com.chrono.commons.exception.DuplicateResourceException;
import com.chrono.commons.exception.ResourceNotFoundException;
import com.chrono.commons.exception.TenantViolationException;
import com.chrono.user.client.AuthServiceClient;
import com.chrono.user.domain.entity.UserProfile;
import com.chrono.user.domain.entity.UserStatus;
import com.chrono.user.domain.repository.UserProfileRepository;
import com.chrono.user.dto.CreateUserRequest;
import com.chrono.user.dto.UpdateUserRequest;
import com.chrono.user.dto.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserProfileRepository userProfileRepository;
    @Mock private AuthServiceClient authServiceClient;

    @InjectMocks
    private UserService userService;

    // -------------------------------------------------------------------------
    // createUser()
    // -------------------------------------------------------------------------

    @Test
    void createUser_happyPath_savesProfileAndRegistersCredentials() {
        when(userProfileRepository.existsByEmail("new@tenant.com")).thenReturn(false);

        CreateUserRequest req = createUserRequest("new@tenant.com", UserRole.TENANT_USER, "pass123");

        UserProfileResponse resp = userService.createUser(req, "tenant-1", UserRole.TENANT_ADMIN.name());

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        UserProfile saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("new@tenant.com");
        assertThat(saved.getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.getTimezone()).isEqualTo("UTC");

        verify(authServiceClient).registerCredential(any());
        assertThat(resp.getEmail()).isEqualTo("new@tenant.com");
    }

    @Test
    void createUser_duplicateEmail_throwsDuplicateResourceException() {
        when(userProfileRepository.existsByEmail("dup@tenant.com")).thenReturn(true);

        CreateUserRequest req = createUserRequest("dup@tenant.com", UserRole.TENANT_USER, "pass");

        assertThatThrownBy(() -> userService.createUser(req, "tenant-1", UserRole.TENANT_ADMIN.name()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void createUser_tenantUserAssigningAdminRole_throwsBusinessException() {
        when(userProfileRepository.existsByEmail(anyString())).thenReturn(false);

        CreateUserRequest req = createUserRequest("x@t.com", UserRole.TENANT_ADMIN, "pass");

        assertThatThrownBy(() ->
                userService.createUser(req, "tenant-1", UserRole.TENANT_USER.name()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient permissions");
    }

    @Test
    void createUser_defaultTimezoneApplied_whenNotProvided() {
        when(userProfileRepository.existsByEmail(anyString())).thenReturn(false);

        CreateUserRequest req = createUserRequest("tz@t.com", UserRole.TENANT_USER, "pass");
        req.setTimezone(null);

        userService.createUser(req, "tenant-1", UserRole.TENANT_ADMIN.name());

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getTimezone()).isEqualTo("UTC");
    }

    // -------------------------------------------------------------------------
    // getUserById()
    // -------------------------------------------------------------------------

    @Test
    void getUserById_sameTenat_returnsProfile() {
        UserProfile profile = validProfile("user-1", "tenant-1");
        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));

        UserProfileResponse resp = userService.getUserById("user-1", "tenant-1", UserRole.TENANT_USER.name());

        assertThat(resp.getUserId()).isEqualTo("user-1");
    }

    @Test
    void getUserById_platformAdmin_bypassesTenantCheck() {
        UserProfile profile = validProfile("user-1", "tenant-1");
        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));

        // PLATFORM_ADMIN from different tenant — should succeed
        UserProfileResponse resp = userService.getUserById("user-1", "other-tenant", UserRole.PLATFORM_ADMIN.name());

        assertThat(resp.getUserId()).isEqualTo("user-1");
    }

    @Test
    void getUserById_crossTenantAccess_throwsTenantViolation() {
        UserProfile profile = validProfile("user-1", "tenant-1");
        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));

        assertThatThrownBy(() ->
                userService.getUserById("user-1", "tenant-2", UserRole.TENANT_ADMIN.name()))
                .isInstanceOf(TenantViolationException.class);
    }

    @Test
    void getUserById_softDeletedUser_throwsResourceNotFoundException() {
        UserProfile profile = validProfile("user-1", "tenant-1");
        profile.setDeletedAt(LocalDateTime.now().minusHours(1));
        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));

        assertThatThrownBy(() ->
                userService.getUserById("user-1", "tenant-1", UserRole.TENANT_USER.name()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserById_unknownUser_throwsResourceNotFoundException() {
        when(userProfileRepository.findByUserId("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.getUserById("ghost", "tenant-1", UserRole.TENANT_USER.name()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // updateUser()
    // -------------------------------------------------------------------------

    @Test
    void updateUser_partialUpdate_onlyChangesProvidedFields() {
        UserProfile profile = validProfile("user-1", "tenant-1");
        profile.setFirstName("OldFirst");
        profile.setPhone("000");

        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setFirstName("NewFirst");
        // phone not provided — should stay as is

        UserProfileResponse resp = userService.updateUser("user-1", req, "tenant-1", UserRole.TENANT_USER.name());

        assertThat(profile.getFirstName()).isEqualTo("NewFirst");
        assertThat(profile.getPhone()).isEqualTo("000"); // unchanged
        assertThat(resp.getFirstName()).isEqualTo("NewFirst");
    }

    @Test
    void updateUser_crossTenantAccess_throwsTenantViolation() {
        UserProfile profile = validProfile("user-1", "tenant-1");
        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));

        assertThatThrownBy(() ->
                userService.updateUser("user-1", new UpdateUserRequest(), "tenant-2", UserRole.TENANT_ADMIN.name()))
                .isInstanceOf(TenantViolationException.class);
    }

    // -------------------------------------------------------------------------
    // deleteUser()
    // -------------------------------------------------------------------------

    @Test
    void deleteUser_setsDeletedAt() {
        UserProfile profile = validProfile("user-1", "tenant-1");
        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));

        userService.deleteUser("user-1", "tenant-1", UserRole.TENANT_ADMIN.name());

        assertThat(profile.getDeletedAt()).isNotNull();
        assertThat(profile.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void deleteUser_alreadyDeleted_throwsResourceNotFoundException() {
        UserProfile profile = validProfile("user-1", "tenant-1");
        profile.setDeletedAt(LocalDateTime.now().minusMinutes(5));
        when(userProfileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));

        assertThatThrownBy(() ->
                userService.deleteUser("user-1", "tenant-1", UserRole.TENANT_ADMIN.name()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // listUsersByTenant()
    // -------------------------------------------------------------------------

    @Test
    void listUsersByTenant_returnsPagedResults() {
        UserProfile p1 = validProfile("u1", "tenant-1");
        UserProfile p2 = validProfile("u2", "tenant-1");

        org.springframework.data.domain.Page<UserProfile> page =
                new PageImpl<>(Arrays.asList(p1, p2));

        when(userProfileRepository.searchByTenant(eq("tenant-1"), isNull(), isNull(), any()))
                .thenReturn(page);

        PageRequest pageReq = new PageRequest();
        pageReq.setPage(0);
        pageReq.setSize(20);

        PageResponse<UserProfileResponse> resp =
                userService.listUsersByTenant("tenant-1", null, null, pageReq, "tenant-1", UserRole.TENANT_ADMIN.name());

        assertThat(resp.getContent()).hasSize(2);
    }

    @Test
    void listUsersByTenant_crossTenantWithoutAdmin_throwsTenantViolation() {
        PageRequest pageReq = new PageRequest();
        pageReq.setPage(0);
        pageReq.setSize(20);

        assertThatThrownBy(() ->
                userService.listUsersByTenant("tenant-1", null, null, pageReq, "tenant-2", UserRole.TENANT_USER.name()))
                .isInstanceOf(TenantViolationException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserProfile validProfile(String userId, String tenantId) {
        UserProfile p = new UserProfile();
        p.setUserId(userId);
        p.setEmail(userId + "@tenant.com");
        p.setFirstName("First");
        p.setLastName("Last");
        p.setTenantId(tenantId);
        p.setRole(UserRole.TENANT_USER);
        p.setStatus(UserStatus.ACTIVE);
        p.setTimezone("UTC");
        return p;
    }

    private CreateUserRequest createUserRequest(String email, UserRole role, String password) {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail(email);
        req.setFirstName("First");
        req.setLastName("Last");
        req.setPassword(password);
        req.setRole(role);
        req.setTimezone("UTC");
        return req;
    }
}
