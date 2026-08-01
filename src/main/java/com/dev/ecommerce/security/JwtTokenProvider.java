package com.dev.ecommerce.security;

import com.dev.ecommerce.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(
                userDetails.getUsername(),
                jwtProperties.getAccessTokenExpirationMs(),
                "access"
        );
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(
                userDetails.getUsername(),
                jwtProperties.getRefreshTokenExpirationMs(),
                "refresh"
        );
    }

    private String buildToken(String subject, long expirationMs, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);

            return username.equals(userDetails.getUsername())
                    && !isTokenExpired(token);

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public String extractTokenType(String token) {
        return extractClaim(
                token,
                claims -> claims.get("type", String.class)
        );
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long getRemainingExpirationMs(String token) {
        Date expiration = extractExpiration(token);

        return Math.max(
                expiration.getTime() - System.currentTimeMillis(),
                0
        );
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token)
                .before(new Date());
    }

    private <T> T extractClaim(
            String token,
            Function<Claims, T> resolver
    ) {
        Claims claims = getClaims(token);

        return resolver.apply(claims);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret()
                .getBytes(StandardCharsets.UTF_8);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}