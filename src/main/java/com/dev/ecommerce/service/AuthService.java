package com.dev.ecommerce.service;

import com.dev.ecommerce.config.JwtProperties;
import com.dev.ecommerce.dto.request.ForgotPasswordRequest;
import com.dev.ecommerce.dto.request.LoginRequest;
import com.dev.ecommerce.dto.request.RefreshTokenRequest;
import com.dev.ecommerce.dto.request.RegisterRequest;
import com.dev.ecommerce.dto.request.ResetPasswordRequest;
import com.dev.ecommerce.dto.response.AuthResponse;
import com.dev.ecommerce.entity.Role;
import com.dev.ecommerce.entity.User;
import com.dev.ecommerce.entity.enums.RoleName;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.repository.RoleRepository;
import com.dev.ecommerce.repository.UserRepository;
import com.dev.ecommerce.security.JwtTokenProvider;
import com.dev.ecommerce.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long PASSWORD_RESET_EXPIRATION_MS = 900_000;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final TokenService tokenService;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered", HttpStatus.CONFLICT);
        }

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new BusinessException("Default role not found", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName()
        );
        user.getRoles().add(userRole);
        userRepository.save(user);

        return buildAuthResponse(new UserPrincipal(user));
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return buildAuthResponse(principal);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!"refresh".equals(jwtTokenProvider.extractTokenType(refreshToken))) {
            throw new BusinessException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }

        String email = jwtTokenProvider.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.UNAUTHORIZED));

        UserPrincipal principal = new UserPrincipal(user);
        String tokenId = jwtTokenProvider.extractTokenId(refreshToken);

        if (!tokenService.isRefreshTokenValid(user.getId(), tokenId, refreshToken)) {
            throw new BusinessException("Refresh token revoked or expired", HttpStatus.UNAUTHORIZED);
        }

        tokenService.revokeRefreshToken(user.getId(), tokenId);
        return buildAuthResponse(principal);
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        if (accessToken != null && "access".equals(jwtTokenProvider.extractTokenType(accessToken))) {
            String accessTokenId = jwtTokenProvider.extractTokenId(accessToken);
            long remainingMs = jwtTokenProvider.getRemainingExpirationMs(accessToken);
            tokenService.blacklistAccessToken(accessTokenId, remainingMs);
        }

        if (refreshToken != null && "refresh".equals(jwtTokenProvider.extractTokenType(refreshToken))) {
            String email = jwtTokenProvider.extractUsername(refreshToken);
            userRepository.findByEmail(email).ifPresent(user -> {
                String tokenId = jwtTokenProvider.extractTokenId(refreshToken);
                tokenService.revokeRefreshToken(user.getId(), tokenId);
            });
        }
    }

    @Transactional(readOnly = true)
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            tokenService.storePasswordResetToken(resetToken, user.getEmail(), PASSWORD_RESET_EXPIRATION_MS);
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = tokenService.getPasswordResetEmail(request.getToken());
        if (email == null) {
            throw new BusinessException("Invalid or expired reset token", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        tokenService.revokePasswordResetToken(request.getToken());
        tokenService.revokeAllRefreshTokens(user.getId());
    }

    private AuthResponse buildAuthResponse(UserPrincipal principal) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);
        String refreshTokenId = jwtTokenProvider.extractTokenId(refreshToken);

        tokenService.storeRefreshToken(principal.getId(), refreshTokenId, refreshToken);

        Set<String> roles = principal.getAuthorities().stream()
                .map(Object::toString)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .collect(Collectors.toSet());

        AuthResponse.UserResponse userResponse = AuthResponse.UserResponse.builder()
                .id(principal.getId())
                .email(principal.getEmail())
                .fullName(principal.getFullName())
                .roles(roles)
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpirationMs() / 1000)
                .user(userResponse)
                .build();
    }
}
