package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.request.BrandRequest;
import com.dev.ecommerce.dto.response.BrandResponse;
import com.dev.ecommerce.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
@Tag(name = "Admin - Brands", description = "Admin CRUD operations for brands")
public class AdminBrandController {

    private final BrandService brandService;

    @GetMapping
    @Operation(summary = "List brands (admin)")
    public ApiResponse<Object> list(Pageable pageable) {
        return ApiResponse.success("Brands fetched", brandService.list(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get brand by id (admin)")
    public ApiResponse<BrandResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Brand fetched", brandService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a brand")
    public ApiResponse<BrandResponse> create(@Valid @RequestBody BrandRequest request) {
        return ApiResponse.success("Brand created", brandService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a brand")
    public ApiResponse<BrandResponse> update(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        return ApiResponse.success("Brand updated", brandService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a brand")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return ApiResponse.success("Brand deleted", null);
    }
}