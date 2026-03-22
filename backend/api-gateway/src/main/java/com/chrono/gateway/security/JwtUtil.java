package com.chrono.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parse and validate a JWT token, returning all claims.
     * Throws JwtException on any validation failure.
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseAndValidate(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    public String extractTenantId(Claims claims) {
        return claims.get("tenantId", String.class);
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public String extractJti(Claims claims) {
        return claims.getId();
    }

    /**
     * Returns remaining TTL in seconds for blacklist expiry alignment.
     */
    public long remainingTtlSeconds(Claims claims) {
        long expiryMs = claims.getExpiration().getTime();
        long nowMs = System.currentTimeMillis();
        long ttlMs = expiryMs - nowMs;
        return ttlMs > 0 ? ttlMs / 1000 : 0L;
    }
}
