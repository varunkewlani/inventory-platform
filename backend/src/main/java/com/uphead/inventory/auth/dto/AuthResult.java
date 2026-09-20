package com.uphead.inventory.auth.dto;

import com.uphead.inventory.users.dto.UserResponse;

public record AuthResult(UserResponse user, AuthTokens tokens) {
}
