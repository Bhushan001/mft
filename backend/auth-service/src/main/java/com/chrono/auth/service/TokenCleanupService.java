package com.chrono.auth.service;

import com.chrono.auth.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    /** Runs nightly at 02:00 to purge expired/revoked refresh tokens */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Running refresh token cleanup");
        refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
        log.info("Refresh token cleanup complete");
    }
}
