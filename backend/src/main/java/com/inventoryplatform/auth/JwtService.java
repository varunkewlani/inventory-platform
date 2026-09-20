package com.inventoryplatform.auth;

import com.inventoryplatform.roles.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies short-lived JWT access tokens. Refresh tokens are
 * deliberately NOT JWTs (see {@link RefreshTokenService}) — they're opaque,
 * server-tracked, and revocable, which a stateless JWT can't be without
 * a denylist.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_ORG = "org";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMAIL = "email";

    private final JwtProperties properties;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, Long organizationId, Role role, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(properties.accessTokenTtlMinutes()));

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_ORG, organizationId.toString())
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_EMAIL, email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    public ParsedAccessToken parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new ParsedAccessToken(
                    Long.valueOf(claims.getSubject()),
                    Long.valueOf(claims.get(CLAIM_ORG, String.class)),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                    claims.get(CLAIM_EMAIL, String.class)
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired access token", e);
        }
    }

    public record ParsedAccessToken(Long userId, Long organizationId, Role role, String email) {
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
