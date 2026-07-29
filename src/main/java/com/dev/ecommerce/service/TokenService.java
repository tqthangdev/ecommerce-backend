package com.dev.ecommerce.service;

import com.dev.ecommerce.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token_blacklist:";
    private static final String PASSWORD_RESET_PREFIX = "password_reset:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    public void storeRefreshToken(Long userId, String tokenId, String refreshToken) {
        String key = refreshTokenKey(userId, tokenId);
        stringRedisTemplate.opsForValue().set(
                key,
                refreshToken,
                jwtProperties.getRefreshTokenExpirationMs(),
                TimeUnit.MILLISECONDS
        );
    }

    public boolean isRefreshTokenValid(Long userId, String tokenId, String refreshToken) {
        String stored = stringRedisTemplate.opsForValue().get(refreshTokenKey(userId, tokenId));
        return refreshToken.equals(stored);
    }

    public void revokeRefreshToken(Long userId, String tokenId) {
        stringRedisTemplate.delete(refreshTokenKey(userId, tokenId));
    }

    public void revokeAllRefreshTokens(Long userId) {
        String pattern = REFRESH_TOKEN_PREFIX + userId + ":*";
        var keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    public void blacklistAccessToken(String tokenId, long remainingMs) {
        if (remainingMs <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                TOKEN_BLACKLIST_PREFIX + tokenId,
                "true",
                Duration.ofMillis(remainingMs)
        );
    }

    public boolean isAccessTokenBlacklisted(String tokenId) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + tokenId)
        );
    }

    public void storePasswordResetToken(String token, String email, long expirationMs) {
        stringRedisTemplate.opsForValue().set(
                PASSWORD_RESET_PREFIX + token,
                email,
                expirationMs,
                TimeUnit.MILLISECONDS
        );
    }

    public String getPasswordResetEmail(String token) {
        return stringRedisTemplate.opsForValue().get(PASSWORD_RESET_PREFIX + token);
    }

    public void revokePasswordResetToken(String token) {
        stringRedisTemplate.delete(PASSWORD_RESET_PREFIX + token);
    }

    private String refreshTokenKey(Long userId, String tokenId) {
        return REFRESH_TOKEN_PREFIX + userId + ":" + tokenId;
    }
}
