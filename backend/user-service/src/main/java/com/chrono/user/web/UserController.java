package com.chrono.user.web;

import com.chrono.commons.constants.ApiConstants;
import com.chrono.commons.dto.ApiResponse;
import com.chrono.commons.dto.PageRequest;
import com.chrono.commons.dto.PageResponse;
import com.chrono.commons.web.BaseController;
import com.chrono.user.domain.entity.UserStatus;
import com.chrono.user.dto.CreateUserRequest;
import com.chrono.user.dto.UpdateUserRequest;
import com.chrono.user.dto.UserProfileResponse;
import com.chrono.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping(ApiConstants.API_V1 + "/users")
@RequiredArgsConstructor
public class UserController extends BaseController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader(ApiConstants.HEADER_TENANT_ID) String tenantId,
            @RequestHeader(ApiConstants.HEADER_USER_ROLE)  String role) {
        UserProfileResponse response = userService.createUser(request, tenantId, role);
        return created(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUser(
            @PathVariable String userId,
            @RequestHeader(ApiConstants.HEADER_TENANT_ID) String tenantId,
            @RequestHeader(ApiConstants.HEADER_USER_ROLE)  String role) {
        UserProfileResponse response = userService.getUserById(userId, tenantId, role);
        return ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserProfileResponse>>> listUsers(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @ModelAttribute PageRequest pageRequest,
            @RequestHeader(ApiConstants.HEADER_TENANT_ID) String callerTenantId,
            @RequestHeader(ApiConstants.HEADER_USER_ROLE)  String callerRole) {

        String targetTenantId = tenantId != null ? tenantId : callerTenantId;
        PageResponse<UserProfileResponse> response =
                userService.listUsersByTenant(targetTenantId, search, status,
                        pageRequest, callerTenantId, callerRole);
        return page(response);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request,
            @RequestHeader(ApiConstants.HEADER_TENANT_ID) String tenantId,
            @RequestHeader(ApiConstants.HEADER_USER_ROLE)  String role) {
        UserProfileResponse response = userService.updateUser(userId, request, tenantId, role);
        return ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable String userId,
            @RequestHeader(ApiConstants.HEADER_TENANT_ID) String tenantId,
            @RequestHeader(ApiConstants.HEADER_USER_ROLE)  String role) {
        userService.deleteUser(userId, tenantId, role);
        return noContent();
    }
}
