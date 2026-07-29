package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.response.CategoryResponse;
import com.dev.ecommerce.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Public category browse APIs")
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List categories")
    public ApiResponse<Object> list(Pageable pageable) {
        return ApiResponse.success("Categories fetched", categoryService.list(pageable));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get category by slug")
    public ApiResponse<CategoryResponse> get(@PathVariable String slug) {
        return ApiResponse.success("Category fetched", categoryService.getBySlug(slug));
    }
}