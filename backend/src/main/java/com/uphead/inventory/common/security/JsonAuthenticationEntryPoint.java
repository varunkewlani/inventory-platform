package com.uphead.inventory.common.security;

import tools.jackson.databind.ObjectMapper;
import com.uphead.inventory.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs outside the normal {@code @RestControllerAdvice} flow (Spring
 * Security's {@code ExceptionTranslationFilter} invokes it directly), so it
 * has to write the standard error envelope itself rather than throwing.
 * Ensures "no/invalid token" reliably comes back as 401, not the 403 Spring
 * Security defaults to when no entry point is configured.
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error("UNAUTHORIZED", "Authentication is required to access this resource"));
    }
}
