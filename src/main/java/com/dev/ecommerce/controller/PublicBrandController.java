package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.response.BrandResponse;
import com.dev.ecommerce.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
@Tag(name = "Brands", description = "Public brand browse APIs")
public class PublicBrandController {

    private final BrandService brandService;

    @GetMapping
    @Operation(summary = "List brands")
    public ApiResponse<Object> list(Pageable pageable) {
        return ApiResponse.success("Brands fetched", brandService.list(pageable));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get brand by slug")
    public ApiResponse<BrandResponse> get(@PathVariable String slug) {
        return ApiResponse.success("Brand fetched", brandService.getBySlug(slug));
    }
}