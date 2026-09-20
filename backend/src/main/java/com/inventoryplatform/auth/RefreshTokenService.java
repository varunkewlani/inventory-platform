package com.inventoryplatform.auth;

import com.inventoryplatform.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Refresh tokens are opaque random strings, not JWTs — only their SHA-256
 * hash is stored, so a database leak doesn't hand out usable tokens. Each
 * successful refresh rotates: the old token is marked revoked and a new one
 * issued. If a token that's already revoked is presented again, that's a
 * signal of theft/replay, so every active session for that user is revoked
 * as a precaution (see {@link #rotate}). Deliberately NOT
 * {@code @Transactional} — the reuse-detection revoke must survive even
 * though the caller throws right after, so it must commit on its own rather
 * than roll back with the enclosing exception.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public IssuedRefreshToken issue(Long userId, String deviceInfo) {
        String rawToken = generateOpaqueToken();
        Instant expiresAt = Instant.now().plus(Duration.ofDays(jwtProperties.refreshTokenTtlDays()));

        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .deviceInfo(deviceInfo)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(entity);

        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    /**
     * Validates and revokes the presented token, returning the user id it
     * belonged to so the caller can issue a fresh pair. Throws on
     * invalid/expired/already-used tokens.
     */
    public Long rotate(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (existing.getRevokedAt() != null) {
            refreshTokenRepository.revokeAllActiveForUser(existing.getUserId(), Instant.now());
            throw new UnauthorizedException("Refresh token was already used; all sessions have been revoked");
        }

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        existing.setRevokedAt(Instant.now());
        refreshTokenRepository.save(existing);
        return existing.getUserId();
    }

    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(rt -> {
            rt.setRevokedAt(Instant.now());
            refreshTokenRepository.save(rt);
        });
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record IssuedRefreshToken(String rawToken, Instant expiresAt) {
    }
}
