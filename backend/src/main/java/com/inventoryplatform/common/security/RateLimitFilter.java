package com.inventoryplatform.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
 */
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMIT = 100;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = "ratelimit:" + request.getRemoteAddr();
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1L) {
            redisTemplate.expire(key, WINDOW);
        }

        if (count != null && count > LIMIT) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"success\":false,\"data\":null,\"meta\":null,\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests, please try again later\"}}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
