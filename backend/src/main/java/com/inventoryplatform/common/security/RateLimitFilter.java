package com.inventoryplatform.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Fixed-window rate limit, keyed by client IP: {@code INCR ratelimit:{ip}},
 * with {@code EXPIRE} set only on the first request in a window. Runs
 * before authentication so it also protects unauthenticated endpoints
 * (login/register) from brute-force, not just authenticated traffic.
 *
 * <p>Fails <b>open</b> if Redis is unreachable — a rate limiter that takes
 * the entire API down when its own dependency has a hiccup is worse than no
 * rate limiter at all. This is also this app's documented answer to the
 * spec's "Redis unavailable" failure scenario for this specific feature.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;
    private final int limit;

    public RateLimitFilter(StringRedisTemplate redisTemplate, int limit) {
        this.redisTemplate = redisTemplate;
        this.limit = limit;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isOverLimit(request)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"success\":false,\"data\":null,\"meta\":null,\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests, please try again later\"}}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isOverLimit(HttpServletRequest request) {
        try {
            String key = "ratelimit:" + request.getRemoteAddr();
            Long count = redisTemplate.opsForValue().increment(key);

            if (count != null && count == 1L) {
                redisTemplate.expire(key, WINDOW);
            }

            return count != null && count > limit;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable, allowing request through unrated: {}", e.getMessage());
            return false;
        }
    }
}
