package com.inventoryplatform.auth;

import com.inventoryplatform.auth.dto.AuthResult;
import com.inventoryplatform.auth.dto.AuthTokens;
import com.inventoryplatform.auth.dto.LoginRequest;
import com.inventoryplatform.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResult register(RegisterRequest request, String deviceInfo);

    AuthResult login(LoginRequest request, String deviceInfo);

    AuthTokens refresh(String rawRefreshToken, String deviceInfo);

    void logout(String rawRefreshToken);
}
