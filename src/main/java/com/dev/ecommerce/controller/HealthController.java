package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Service health check")
public class HealthController {

    @GetMapping
    @Operation(summary = "Check service health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(
                "Service is healthy",
                Map.of(
                        "status", "UP",
                        "timestamp", LocalDateTime.now()
                )
        );
    }
}
