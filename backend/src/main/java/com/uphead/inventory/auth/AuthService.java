package com.uphead.inventory.auth;

import com.uphead.inventory.auth.dto.AuthResult;
import com.uphead.inventory.auth.dto.AuthTokens;
import com.uphead.inventory.auth.dto.LoginRequest;
import com.uphead.inventory.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResult register(RegisterRequest request, String deviceInfo);

    AuthResult login(LoginRequest request, String deviceInfo);

    AuthTokens refresh(String rawRefreshToken, String deviceInfo);

    void logout(String rawRefreshToken);
}
