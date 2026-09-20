package com.inventoryplatform.auth;

import com.inventoryplatform.auth.dto.AccessTokenResponse;
import com.inventoryplatform.auth.dto.AuthResult;
import com.inventoryplatform.auth.dto.AuthTokens;
import com.inventoryplatform.auth.dto.LoginRequest;
import com.inventoryplatform.auth.dto.LoginResponse;
import com.inventoryplatform.auth.dto.RegisterRequest;
import com.inventoryplatform.common.exception.UnauthorizedException;
import com.inventoryplatform.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        AuthResult result = authService.register(request, deviceInfo(httpRequest));
        setRefreshCookie(httpResponse, result.tokens());
        return ApiResponse.success(LoginResponse.from(result));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        AuthResult result = authService.login(request, deviceInfo(httpRequest));
        setRefreshCookie(httpResponse, result.tokens());
        return ApiResponse.success(LoginResponse.from(result));
    }

    @PostMapping("/refresh")
    public ApiResponse<AccessTokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshTokenCookie,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (refreshTokenCookie == null) {
            throw new UnauthorizedException("No refresh token present");
        }
        AuthTokens tokens = authService.refresh(refreshTokenCookie, deviceInfo(httpRequest));
        setRefreshCookie(httpResponse, tokens);
        return ApiResponse.success(new AccessTokenResponse(tokens.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshTokenCookie,
            HttpServletResponse httpResponse) {
        if (refreshTokenCookie != null) {
            authService.logout(refreshTokenCookie);
        }
        clearRefreshCookie(httpResponse);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private void setRefreshCookie(HttpServletResponse response, AuthTokens tokens) {
        long maxAgeSeconds = java.time.Duration.between(java.time.Instant.now(), tokens.refreshTokenExpiresAt()).getSeconds();
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, tokens.refreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Math.max(maxAgeSeconds, 0))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String deviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null ? "unknown" : userAgent;
    }
}
