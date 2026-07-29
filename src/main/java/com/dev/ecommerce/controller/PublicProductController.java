package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.request.ProductSearchRequest;
import com.dev.ecommerce.dto.response.PageResponse;
import com.dev.ecommerce.dto.response.ProductResponse;
import com.dev.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Products", description = "Public product browse, search, and filter APIs")
public class PublicProductController {

    private final ProductService productService;

    @GetMapping("/search")
    @Operation(summary = "Search and filter products")
    public ApiResponse<PageResponse<ProductResponse>> search(@Valid @ModelAttribute ProductSearchRequest request) {
        return ApiResponse.success("Products fetched", productService.search(request));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get product detail by slug")
    public ApiResponse<ProductResponse> get(@PathVariable String slug) {
        return ApiResponse.success("Product fetched", productService.getBySlug(slug));
    }
}