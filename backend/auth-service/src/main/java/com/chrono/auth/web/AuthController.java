package com.chrono.auth.web;

import com.chrono.auth.dto.*;
import com.chrono.auth.service.AuthService;
import com.chrono.commons.constants.ApiConstants;
import com.chrono.commons.dto.ApiResponse;
import com.chrono.commons.web.BaseController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping(ApiConstants.API_V1 + "/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refresh(request);
        return ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request,
            @RequestHeader(value = "X-Jti", required = false) String jti,
            @RequestHeader(value = "X-Token-Expiry", required = false) String tokenExpiry) {
        authService.logout(request, jti, tokenExpiry);
        return noContent();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        // Always 204 — don't reveal whether email exists
        return noContent();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return noContent();
    }

    /** Internal endpoint — called by user-service via Feign, not exposed to external clients */
    @PostMapping("/internal/register")
    public ResponseEntity<ApiResponse<Void>> registerCredential(
            @Valid @RequestBody RegisterCredentialRequest request) {
        authService.registerCredential(request);
        return noContent();
    }

    /** Internal: soft-delete credentials when user is deleted */
    @DeleteMapping("/internal/credentials/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteCredential(@PathVariable String userId) {
        authService.disableCredential(userId);
        return noContent();
    }
}
