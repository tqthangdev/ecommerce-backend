package com.dev.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Configuration
public class RateLimitingConfig {

    @Bean
    public RedisScript<List> rateLimitScript() {
        String script = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current = tonumber(redis.call('GET', key) or '0')
            if current >= limit then
                return {0, current, limit}
            else
                redis.call('INCR', key)
                if current == 0 then
                    redis.call('EXPIRE', key, window)
                end
                return {1, current + 1, limit}
            end
            """;
        return RedisScript.of(script, List.class);
    }

    @Bean
    public StringRedisTemplate rateLimitRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}

@Slf4j
@Component
@RequiredArgsConstructor
class RateLimitingFilter extends OncePerRequestFilter {

    private final StringRedisTemplate rateLimitRedisTemplate;
    private final RedisScript<List> rateLimitScript;

    private static final int AUTH_LIMIT = 5;
    private static final int API_LIMIT = 100;
    private static final int WINDOW_SECONDS = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = getClientIp(request);
        boolean isLogin = path.equals("/api/auth/login");

        String key = "rate:" + (isLogin ? "auth:" : "api:") + ip;
        int limit = isLogin ? AUTH_LIMIT : API_LIMIT;

        try {
            @SuppressWarnings("unchecked")
            List<Long> result = rateLimitRedisTemplate.execute(
                    rateLimitScript,
                    List.of(key),
                    String.valueOf(limit),
                    String.valueOf(WINDOW_SECONDS)
            );

            if (result != null && result.get(0) == 0) {
                sendTooManyRequests(response);
                return;
            }

            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            if (result != null) {
                response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - result.get(1))));
            }
        } catch (Exception e) {
            log.warn("Rate limiting check failed for key={}", key, e);
            if (isLogin) {
                sendTooManyRequests(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"Too many requests. Try again later.\"}"
        );
    }

    private static final List<String> TRUSTED_PROXIES = List.of(
            "127.0.0.1", "0:0:0:0:0:0:0:1"
    );

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (TRUSTED_PROXIES.contains(remoteAddr)) {
            String xfwd = request.getHeader("X-Forwarded-For");
            if (xfwd != null && !xfwd.isBlank()) {
                return xfwd.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }
}