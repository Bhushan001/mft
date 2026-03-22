package com.chrono.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Blacklists a JWT by its JTI (JWT ID) for its remaining lifetime.
     *
     * @param jti          the unique JWT identifier
     * @param ttlSeconds   remaining seconds until token natural expiry
     */
    public void blacklist(String jti, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            log.debug("Token already expired, skipping blacklist: jti={}", jti);
            return;
        }
        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
        log.info("Token blacklisted: jti={}, ttlSeconds={}", jti, ttlSeconds);
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }
}
