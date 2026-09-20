package com.inventoryplatform.common.tenant;

import com.inventoryplatform.common.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Wired into the Spring Security chain (via {@code addFilterAfter}, see
 * {@code SecurityConfig}) immediately after {@code JwtAuthFilter}, so it
 * only ever sees requests that are already authenticated. Copies
 * organizationId/userId/role from the principal into {@link TenantContext}
 * for the duration of the request, then clears it — the {@code finally} is
 * load-bearing since servlet containers reuse request-handling threads.
 */
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                TenantContext.set(principal.organizationId(), principal.userId(), principal.role());
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
