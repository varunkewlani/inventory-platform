package com.uphead.inventory.auth.dto;

import java.time.Instant;

public record AuthTokens(String accessToken, String refreshToken, Instant refreshTokenExpiresAt) {
}
