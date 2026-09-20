package com.inventoryplatform.common.security;

import com.inventoryplatform.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Counterpart to {@link JsonAuthenticationEntryPoint}: fires when a request
 * IS authenticated but lacks the required authority (Spring Security's own
 * {@code hasRole}/{@code hasAuthority} checks, if any are ever added here —
 * {@code PermissionAspect} throws {@code ForbiddenException} directly and
 * goes through {@code GlobalExceptionHandler} instead, so this mostly exists
 * for completeness/defense-in-depth).
 */
@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error("FORBIDDEN", "You do not have permission to perform this action"));
    }
}
