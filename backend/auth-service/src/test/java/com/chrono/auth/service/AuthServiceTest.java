package com.chrono.auth.service;

import com.chrono.auth.domain.entity.RefreshToken;
import com.chrono.auth.domain.entity.UserCredential;
import com.chrono.auth.domain.repository.RefreshTokenRepository;
import com.chrono.auth.domain.repository.UserCredentialRepository;
import com.chrono.auth.dto.*;
import com.chrono.commons.enums.UserRole;
import com.chrono.commons.exception.BusinessException;
import com.chrono.commons.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserCredentialRepository credentialRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private TokenBlacklistService blacklistService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiryMs", 604800000L);
    }

    // -------------------------------------------------------------------------
    // login()
    // -------------------------------------------------------------------------

    @Test
    void login_happyPath_returnsTokens() {
        UserCredential cred = validCredential();
        LoginRequest req = new LoginRequest();
        req.setEmail("user@tenant.com");
        req.setPassword("secret");

        when(credentialRepository.findByEmail("user@tenant.com")).thenReturn(Optional.of(cred));
        when(passwordEncoder.matches("secret", "hashed-secret")).thenReturn(true);
        when(jwtService.generateAccessToken(cred)).thenReturn("access-token");
        when(jwtService.generateRawRefreshToken()).thenReturn("raw-refresh");
        when(jwtService.getAccessTokenExpiryMs()).thenReturn(900000L);

        LoginResponse resp = authService.login(req);

        assertThat(resp.getAccessToken()).isEqualTo("access-token");
        assertThat(resp.getRefreshToken()).isEqualTo("raw-refresh");
        assertThat(resp.getTokenType()).isEqualTo("Bearer");
        assertThat(resp.getUser().getEmail()).isEqualTo("user@tenant.com");

        verify(credentialRepository).resetFailedAttempts(cred.getUserId());
        verify(credentialRepository).updateLastLogin(eq(cred.getUserId()), any(LocalDateTime.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_unknownEmail_throwsBusinessException() {
        when(credentialRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        LoginRequest req = loginRequest("unknown@x.com", "pass");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_disabledAccount_throwsBusinessException() {
        UserCredential cred = validCredential();
        cred.setEnabled(false);
        when(credentialRepository.findByEmail(anyString())).thenReturn(Optional.of(cred));

        assertThatThrownBy(() -> authService.login(loginRequest("user@tenant.com", "any")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void login_lockedAccount_throwsBusinessException() {
        UserCredential cred = validCredential();
        cred.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        when(credentialRepository.findByEmail(anyString())).thenReturn(Optional.of(cred));

        assertThatThrownBy(() -> authService.login(loginRequest("user@tenant.com", "any")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void login_wrongPassword_incrementsFailedAttempts() {
        UserCredential cred = validCredential();
        when(credentialRepository.findByEmail(anyString())).thenReturn(Optional.of(cred));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("user@tenant.com", "wrong")))
                .isInstanceOf(BusinessException.class);

        assertThat(cred.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void login_fiveFailedAttempts_locksAccount() {
        UserCredential cred = validCredential();
        cred.setFailedLoginAttempts(4); // will reach 5 on this call
        when(credentialRepository.findByEmail(anyString())).thenReturn(Optional.of(cred));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("user@tenant.com", "wrong")))
                .isInstanceOf(BusinessException.class);

        assertThat(cred.getLockedUntil()).isNotNull();
        assertThat(cred.getLockedUntil()).isAfter(LocalDateTime.now());
    }

    // -------------------------------------------------------------------------
    // refresh()
    // -------------------------------------------------------------------------

    @Test
    void refresh_validToken_returnsNewTokens() {
        UserCredential cred = validCredential();
        RefreshToken rt = validRefreshToken(cred.getUserId());

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(rt));
        when(credentialRepository.findByUserId(cred.getUserId())).thenReturn(Optional.of(cred));
        when(jwtService.generateAccessToken(cred)).thenReturn("new-access");
        when(jwtService.generateRawRefreshToken()).thenReturn("new-refresh");
        when(jwtService.getAccessTokenExpiryMs()).thenReturn(900000L);

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("old-raw-token");

        LoginResponse resp = authService.refresh(req);

        assertThat(resp.getAccessToken()).isEqualTo("new-access");
        assertThat(rt.isRevoked()).isTrue(); // old token rotated
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refresh_expiredToken_throwsBusinessException() {
        RefreshToken rt = new RefreshToken();
        rt.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        rt.setRevoked(false);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(rt));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("expired-token");

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired or revoked");
    }

    @Test
    void refresh_revokedToken_throwsBusinessException() {
        RefreshToken rt = validRefreshToken("user-1");
        rt.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(rt));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("revoked-token");

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired or revoked");
    }

    @Test
    void refresh_unknownToken_throwsBusinessException() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("ghost-token");

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    // -------------------------------------------------------------------------
    // logout()
    // -------------------------------------------------------------------------

    @Test
    void logout_withValidJti_blacklistsAccessToken() {
        RefreshToken rt = validRefreshToken("user-1");
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(rt));

        LogoutRequest req = new LogoutRequest();
        req.setRefreshToken("raw-refresh");

        long futureExpiry = System.currentTimeMillis() + 60_000;
        authService.logout(req, "jti-abc", String.valueOf(futureExpiry));

        verify(blacklistService).blacklist(eq("jti-abc"), longThat(ttl -> ttl > 0));
        assertThat(rt.isRevoked()).isTrue();
    }

    @Test
    void logout_withNullJti_skipsBlacklisting() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        LogoutRequest req = new LogoutRequest();
        req.setRefreshToken("any-token");

        authService.logout(req, null, null);

        verifyNoInteractions(blacklistService);
    }

    // -------------------------------------------------------------------------
    // registerCredential()
    // -------------------------------------------------------------------------

    @Test
    void registerCredential_newEmail_savesCredential() {
        when(credentialRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed-pass");

        RegisterCredentialRequest req = RegisterCredentialRequest.builder()
                .userId("u-1")
                .email("new@tenant.com")
                .password("pass123")
                .role(UserRole.TENANT_USER)
                .tenantId("t-1")
                .build();

        authService.registerCredential(req);

        ArgumentCaptor<UserCredential> captor = ArgumentCaptor.forClass(UserCredential.class);
        verify(credentialRepository).save(captor.capture());

        UserCredential saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@tenant.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-pass");
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void registerCredential_duplicateEmail_throwsDuplicateResourceException() {
        when(credentialRepository.existsByEmail("dup@tenant.com")).thenReturn(true);

        RegisterCredentialRequest req = RegisterCredentialRequest.builder()
                .userId("u-2")
                .email("dup@tenant.com")
                .password("pass")
                .role(UserRole.TENANT_USER)
                .tenantId("t-1")
                .build();

        assertThatThrownBy(() -> authService.registerCredential(req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // -------------------------------------------------------------------------
    // resetPassword()
    // -------------------------------------------------------------------------

    @Test
    void resetPassword_validToken_updatesHashAndRevokesTokens() {
        UserCredential cred = validCredential();
        cred.setPasswordResetToken("reset-token-123");
        cred.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(30));

        when(credentialRepository.findByPasswordResetToken("reset-token-123")).thenReturn(Optional.of(cred));
        when(passwordEncoder.encode("newPass!")).thenReturn("new-hash");

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("reset-token-123");
        req.setNewPassword("newPass!");

        authService.resetPassword(req);

        assertThat(cred.getPasswordHash()).isEqualTo("new-hash");
        assertThat(cred.getPasswordResetToken()).isNull();
        assertThat(cred.getFailedLoginAttempts()).isEqualTo(0);
        verify(refreshTokenRepository).revokeAllForUser(cred.getUserId());
    }

    @Test
    void resetPassword_expiredToken_throwsBusinessException() {
        UserCredential cred = validCredential();
        cred.setPasswordResetToken("old-token");
        cred.setPasswordResetTokenExpiry(LocalDateTime.now().minusMinutes(5));

        when(credentialRepository.findByPasswordResetToken("old-token")).thenReturn(Optional.of(cred));

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("old-token");
        req.setNewPassword("newPass!");

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void resetPassword_unknownToken_throwsBusinessException() {
        when(credentialRepository.findByPasswordResetToken(anyString())).thenReturn(Optional.empty());

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("ghost");
        req.setNewPassword("pass");

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid");
    }

    // -------------------------------------------------------------------------
    // disableCredential()
    // -------------------------------------------------------------------------

    @Test
    void disableCredential_existingUser_disablesAndRevokesTokens() {
        UserCredential cred = validCredential();
        when(credentialRepository.findByUserId(cred.getUserId())).thenReturn(Optional.of(cred));

        authService.disableCredential(cred.getUserId());

        assertThat(cred.isEnabled()).isFalse();
        verify(refreshTokenRepository).revokeAllForUser(cred.getUserId());
    }

    @Test
    void disableCredential_unknownUser_doesNothing() {
        when(credentialRepository.findByUserId("ghost")).thenReturn(Optional.empty());

        authService.disableCredential("ghost");

        verifyNoInteractions(refreshTokenRepository);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserCredential validCredential() {
        UserCredential cred = new UserCredential();
        cred.setUserId("user-uuid-1");
        cred.setEmail("user@tenant.com");
        cred.setPasswordHash("hashed-secret");
        cred.setRole(UserRole.TENANT_USER);
        cred.setTenantId("tenant-1");
        cred.setEnabled(true);
        return cred;
    }

    private RefreshToken validRefreshToken(String userId) {
        RefreshToken rt = new RefreshToken();
        rt.setTokenHash("hashed-token");
        rt.setUserId(userId);
        rt.setTenantId("tenant-1");
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        rt.setRevoked(false);
        return rt;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }
}
