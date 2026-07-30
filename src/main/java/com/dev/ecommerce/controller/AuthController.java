package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.request.ForgotPasswordRequest;
import com.dev.ecommerce.dto.request.LoginRequest;
import com.dev.ecommerce.dto.request.RefreshTokenRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, logout, password reset")
public class AuthController {

    private final AuthService authService;

    private static final long ACCESS_TOKEN_MAX_AGE = 900;
    private static final long REFRESH_TOKEN_MAX_AGE = 604800;

    private void setCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie atCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(false)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(ACCESS_TOKEN_MAX_AGE)
                .build();

        ResponseCookie rtCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(false)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(REFRESH_TOKEN_MAX_AGE)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());
    }

    private void clearCookies(HttpServletResponse response) {
        ResponseCookie atCookie = ResponseCookie.from("access_token", "")
                .httpOnly(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        ResponseCookie rtCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());
    }

    private String extractToken(Cookie[] cookies, String name) {
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
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
        setCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ApiResponse.success("Registration successful", authResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request);
        setCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ApiResponse.success("Login successful", authResponse);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ApiResponse<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        String refreshToken = extractToken(cookies, "refresh_token");
        String accessToken = extractToken(cookies, "access_token");
        AuthResponse authResponse = authService.refreshToken(refreshToken, accessToken);
        setCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ApiResponse.success("Token refreshed", authResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke tokens")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        String accessToken = extractToken(cookies, "access_token");
        String refreshToken = extractToken(cookies, "refresh_token");
        authService.logout(accessToken, refreshToken);
        clearCookies(response);
        return ApiResponse.success("Logout successful", null);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success("If the email exists, a password reset link has been sent", null);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success("Password reset successful", null);
    }
}
