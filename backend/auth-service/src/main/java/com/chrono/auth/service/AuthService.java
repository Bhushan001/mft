package com.chrono.auth.service;

import com.chrono.auth.domain.entity.RefreshToken;
import com.chrono.auth.domain.entity.UserCredential;
import com.chrono.auth.domain.repository.RefreshTokenRepository;
import com.chrono.auth.domain.repository.UserCredentialRepository;
import com.chrono.auth.dto.*;
import com.chrono.commons.exception.BusinessException;
import com.chrono.commons.exception.DuplicateResourceException;
import com.chrono.commons.exception.ResourceNotFoundException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    private final UserCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiry-ms:604800000}")
    private long refreshTokenExpiryMs;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        UserCredential credential = credentialRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (!credential.isEnabled()) {
            throw new BusinessException("Account is disabled. Contact your administrator.");
        }

        if (credential.isAccountLocked()) {
            throw new BusinessException("Account is temporarily locked. Try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
            handleFailedLogin(credential);
            throw new BusinessException("Invalid email or password");
        }

        // Reset failed attempts on success
        credentialRepository.resetFailedAttempts(credential.getUserId());
        credentialRepository.updateLastLogin(credential.getUserId(), LocalDateTime.now());

        String accessToken = jwtService.generateAccessToken(credential);
        String rawRefreshToken = jwtService.generateRawRefreshToken();

        saveRefreshToken(rawRefreshToken, credential);

        log.info("User logged in: userId={}, tenantId={}", credential.getUserId(), credential.getTenantId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiryMs() / 1000)
                .user(LoginResponse.UserInfo.builder()
                        .id(credential.getUserId())
                        .email(credential.getEmail())
                        .role(credential.getRole())
                        .tenantId(credential.getTenantId())
                        .build())
                .build();
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new BusinessException("Refresh token is expired or revoked");
        }

        UserCredential credential = credentialRepository.findByUserId(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", refreshToken.getUserId()));

        if (!credential.isEnabled()) {
            throw new BusinessException("Account is disabled");
        }

        // Rotate refresh token
        refreshToken.setRevoked(true);

        String newAccessToken = jwtService.generateAccessToken(credential);
        String newRawRefreshToken = jwtService.generateRawRefreshToken();
        saveRefreshToken(newRawRefreshToken, credential);

        log.info("Token refreshed for userId={}", credential.getUserId());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiryMs() / 1000)
                .user(LoginResponse.UserInfo.builder()
                        .id(credential.getUserId())
                        .email(credential.getEmail())
                        .role(credential.getRole())
                        .tenantId(credential.getTenantId())
                        .build())
                .build();
    }

    @Transactional
    public void logout(LogoutRequest request, String jti, String tokenExpiryHeader) {
        // Blacklist the access token
        if (jti != null && !jti.trim().isEmpty()) {
            long ttlSeconds = computeRemainingTtl(tokenExpiryHeader);
            blacklistService.blacklist(jti, ttlSeconds);
        }

        // Revoke the refresh token
        String tokenHash = hashToken(request.getRefreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(rt -> rt.setRevoked(true));

        log.info("User logged out: jti={}", jti);
    }

    @Transactional
    public void registerCredential(RegisterCredentialRequest request) {
        if (credentialRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("UserCredential", "email", request.getEmail());
        }

        UserCredential credential = new UserCredential();
        credential.setUserId(request.getUserId());
        credential.setEmail(request.getEmail());
        credential.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        credential.setRole(request.getRole());
        credential.setTenantId(request.getTenantId());
        credential.setEnabled(true);

        credentialRepository.save(credential);
        log.info("Credentials registered: userId={}, tenantId={}", request.getUserId(), request.getTenantId());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success — don't leak whether email exists
        credentialRepository.findByEmail(request.getEmail()).ifPresent(credential -> {
            String token = UUID.randomUUID().toString();
            credential.setPasswordResetToken(token);
            credential.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
            log.info("Password reset token generated for userId={}", credential.getUserId());
            // TODO: emit event / call notification service to send email
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UserCredential credential = credentialRepository
                .findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new BusinessException("Invalid or expired reset token"));

        if (LocalDateTime.now().isAfter(credential.getPasswordResetTokenExpiry())) {
            throw new BusinessException("Reset token has expired");
        }

        credential.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        credential.setPasswordResetToken(null);
        credential.setPasswordResetTokenExpiry(null);
        credential.setFailedLoginAttempts(0);
        credential.setLockedUntil(null);

        // Revoke all refresh tokens on password change (force re-login on all devices)
        refreshTokenRepository.revokeAllForUser(credential.getUserId());

        log.info("Password reset for userId={}", credential.getUserId());
    }

    @Transactional
    public void disableCredential(String userId) {
        credentialRepository.findByUserId(userId).ifPresent(credential -> {
            credential.setEnabled(false);
            refreshTokenRepository.revokeAllForUser(userId);
            log.info("Credentials disabled for userId={}", userId);
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void handleFailedLogin(UserCredential credential) {
        int attempts = credential.getFailedLoginAttempts() + 1;
        credential.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            credential.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Account locked due to failed attempts: userId={}", credential.getUserId());
        }
    }

    private void saveRefreshToken(String rawToken, UserCredential credential) {
        RefreshToken rt = new RefreshToken();
        rt.setTokenHash(hashToken(rawToken));
        rt.setUserId(credential.getUserId());
        rt.setTenantId(credential.getTenantId());
        rt.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiryMs / 1000));
        refreshTokenRepository.save(rt);
    }

    private String hashToken(String token) {
        return DigestUtils.md5DigestAsHex(token.getBytes(StandardCharsets.UTF_8));
    }

    private long computeRemainingTtl(String tokenExpiryHeader) {
        if (tokenExpiryHeader == null || tokenExpiryHeader.trim().isEmpty()) return 0L;
        try {
            long expiryMs = Long.parseLong(tokenExpiryHeader);
            long remaining = (expiryMs - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
