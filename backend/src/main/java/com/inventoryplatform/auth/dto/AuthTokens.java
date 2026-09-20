package com.inventoryplatform.auth.dto;

import java.time.Instant;

public record AuthTokens(String accessToken, String refreshToken, Instant refreshTokenExpiresAt) {
}
