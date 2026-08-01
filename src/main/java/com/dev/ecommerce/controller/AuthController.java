package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.config.JwtProperties;
import com.dev.ecommerce.dto.request.ForgotPasswordRequest;
import com.dev.ecommerce.dto.request.LoginRequest;
import com.dev.ecommerce.dto.request.RegisterRequest;
import com.dev.ecommerce.dto.request.ResetPasswordRequest;
import com.dev.ecommerce.dto.response.AuthResponse;
import com.dev.ecommerce.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "Register, login, token refresh, logout, password reset"
)
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    private void setRefreshTokenCookie(
            HttpServletResponse response,
            String refreshToken
    ) {
        ResponseCookie cookie = ResponseCookie.from(
                    "refresh_token",
                    refreshToken
                )
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMillis(
                        jwtProperties.getRefreshTokenExpirationMs()
                ))
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    private void clearRefreshTokenCookie(
            HttpServletResponse response
    ) {
        ResponseCookie cookie = ResponseCookie.from(
                    "refresh_token",
                    ""
                )
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    // Set cookie xong thì xoá refreshToken khỏi object trước khi trả về client,
    // tránh lộ token qua response body (làm mất tác dụng httpOnly).
    private AuthResponse sanitizeAuthResponse(AuthResponse authResponse) {
        authResponse.setRefreshToken(null);
        return authResponse;
    }

    private String extractToken(
            Cookie[] cookies,
            String name
    ) {
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private String extractAccessToken(
            HttpServletRequest request
    ) {
        String authorization = request.getHeader(
                HttpHeaders.AUTHORIZATION
        );

        if (authorization != null &&
                authorization.startsWith("Bearer ")) {

            return authorization.substring(7);
        }

        return null;
    }


    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    public ApiResponse<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.register(request);

        setRefreshTokenCookie(
                response,
                authResponse.getRefreshToken()
        );

        return ApiResponse.success(
                "Registration successful",
                sanitizeAuthResponse(authResponse)
        );
    }


    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request);

        setRefreshTokenCookie(
                response,
                authResponse.getRefreshToken()
        );

        return ApiResponse.success(
                "Login successful",
                sanitizeAuthResponse(authResponse)
        );
    }


    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ApiResponse<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractToken(
                request.getCookies(),
                "refresh_token"
        );

        AuthResponse authResponse =
                authService.refreshToken(refreshToken);

        setRefreshTokenCookie(
                response,
                authResponse.getRefreshToken()
        );

        return ApiResponse.success(
                "Token refreshed",
                sanitizeAuthResponse(authResponse)
        );
    }


    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke token")
    public ApiResponse<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String accessToken = extractAccessToken(request);

        String refreshToken = extractToken(
                request.getCookies(),
                "refresh_token"
        );

        authService.logout(
                accessToken,
                refreshToken
        );

        clearRefreshTokenCookie(response);

        return ApiResponse.success(
                "Logout successful",
                null
        );
    }


    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public ApiResponse<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request);

        return ApiResponse.success(
                "If the email exists, a password reset link has been sent",
                null
        );
    }


    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);

        return ApiResponse.success(
                "Password reset successful",
                null
        );
    }
}