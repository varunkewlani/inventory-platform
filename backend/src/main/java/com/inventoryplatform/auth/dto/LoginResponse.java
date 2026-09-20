package com.inventoryplatform.auth.dto;

import com.inventoryplatform.users.dto.UserResponse;

/**
 * What actually goes in the response body — the refresh token never does,
 * it's set as an httpOnly cookie by the controller.
 */
public record LoginResponse(String accessToken, UserResponse user) {
    public static LoginResponse from(AuthResult result) {
        return new LoginResponse(result.tokens().accessToken(), result.user());
    }
}
