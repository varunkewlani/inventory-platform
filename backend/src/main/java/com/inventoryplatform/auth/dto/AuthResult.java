package com.inventoryplatform.auth.dto;

import com.inventoryplatform.users.dto.UserResponse;

public record AuthResult(UserResponse user, AuthTokens tokens) {
}
