package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.request.PromotionRequest;
import com.dev.ecommerce.dto.response.PromotionResponse;
import com.dev.ecommerce.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
@Tag(name = "Admin - Promotions", description = "Admin promotion management")
public class AdminPromotionController {

    private final PromotionService promotionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a promotion")
    public ApiResponse<PromotionResponse> create(@Valid @RequestBody PromotionRequest request) {
        return ApiResponse.success("Promotion created", promotionService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a promotion")
    public ApiResponse<PromotionResponse> update(@PathVariable Long id, @Valid @RequestBody PromotionRequest request) {
        return ApiResponse.success("Promotion updated", promotionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a promotion")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ApiResponse.success("Promotion deleted", null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get promotion by id")
    public ApiResponse<PromotionResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Promotion fetched", promotionService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List all promotions")
    public ApiResponse<List<PromotionResponse>> list() {
        return ApiResponse.success("Promotions fetched", promotionService.list());
    }
}
