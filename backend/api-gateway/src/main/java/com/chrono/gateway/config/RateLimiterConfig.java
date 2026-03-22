package com.chrono.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Key resolvers for Spring Cloud Gateway Redis rate limiter.
 * - tenantKeyResolver: rate-limits per tenant (X-Tenant-Id header)
 * - ipKeyResolver: fallback, rate-limits by remote IP
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Primary resolver: per-tenant rate limiting.
     * Falls back to IP when header is absent (e.g. public/auth endpoints).
     */
    @Bean
    @Primary
    public KeyResolver tenantKeyResolver() {
        return exchange -> {
            String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
            if (tenantId != null && !tenantId.trim().isEmpty()) {
                return Mono.just("tenant:" + tenantId);
            }
            // fallback to remote address
            return Mono.just("ip:" + resolveRemoteIp(exchange));
        };
    }

    /**
     * IP-based resolver bean (can be referenced by name in route config).
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just("ip:" + resolveRemoteIp(exchange));
    }

    private String resolveRemoteIp(org.springframework.web.server.ServerWebExchange exchange) {
        // Respect X-Forwarded-For when behind a reverse proxy
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        java.net.InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }
}
