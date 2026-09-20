package com.uphead.inventory.common.security;

import com.uphead.inventory.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads {@code Authorization: Bearer <token>}, verifies it, and — on
 * success — populates the Spring Security context with a
 * {@link UserPrincipal}. An invalid/expired token is treated as "not
 * authenticated" rather than rejected outright here; whether that matters
 * depends on the endpoint, which {@code SecurityConfig}'s authorization
 * rules decide.
 */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                JwtService.ParsedAccessToken parsed = jwtService.parseAccessToken(header.substring(7));
                UserPrincipal principal = new UserPrincipal(
                        parsed.userId(), parsed.organizationId(), parsed.email(), parsed.role());

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + parsed.role().name()));
                var authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (JwtService.InvalidTokenException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
