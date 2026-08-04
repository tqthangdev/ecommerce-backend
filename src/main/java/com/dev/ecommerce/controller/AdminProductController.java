package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.request.ProductRequest;
import com.dev.ecommerce.dto.request.ProductSearchRequest;
import com.dev.ecommerce.dto.request.ProductVariantRequest;
import com.dev.ecommerce.dto.request.ProductImageUrlRequest;
import com.dev.ecommerce.dto.response.PageResponse;
import com.dev.ecommerce.dto.response.ProductImageResponse;
import com.dev.ecommerce.dto.response.ProductResponse;
import com.dev.ecommerce.dto.response.ProductVariantResponse;
import com.dev.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
@Tag(name = "Admin - Products", description = "Admin CRUD operations for products, variants, and images")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all products (admin)")
    public ApiResponse<PageResponse<ProductResponse>> getAll(@Valid @ModelAttribute ProductSearchRequest request) {
        return ApiResponse.success("Products fetched", productService.search(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product detail by id (admin)")
    public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.success("Product fetched", productService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product")
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.success("Product created", productService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success("Product updated", productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.success("Product deleted", null);
    }

    @PostMapping("/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a variant to a product")
    public ApiResponse<ProductVariantResponse> addVariant(
            @PathVariable Long id,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        return ApiResponse.success("Variant added", productService.addVariant(id, request));
    }

    @PutMapping("/variants/{variantId}")
    @Operation(summary = "Update a variant")
    public ApiResponse<ProductVariantResponse> updateVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        return ApiResponse.success("Variant updated", productService.updateVariant(variantId, request));
    }

    @DeleteMapping("/variants/{variantId}")
    @Operation(summary = "Remove a variant")
    public ApiResponse<Void> removeVariant(@PathVariable Long variantId) {
        productService.removeVariant(variantId);
        return ApiResponse.success("Variant removed", null);
    }

    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a product image")
    public ApiResponse<ProductImageResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success("Image uploaded", productService.uploadImage(id, file));
    }

    @DeleteMapping("/images/{imageId}")
    @Operation(summary = "Remove a product image")
    public ApiResponse<Void> removeImage(@PathVariable Long imageId) {
        productService.removeImage(imageId);
        return ApiResponse.success("Image removed", null);
    }

    @PutMapping("/images/{imageId}/primary")
    @Operation(summary = "Set image as primary")
    public ApiResponse<ProductImageResponse> setPrimaryImage(@PathVariable Long imageId) {
        return ApiResponse.success("Primary image updated", productService.setPrimaryImage(imageId));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a product image by URL")
    public ApiResponse<ProductImageResponse> addImageByUrl(
            @PathVariable Long id,
            @Valid @RequestBody ProductImageUrlRequest request
    ) {
        return ApiResponse.success("Image added", productService.addImageByUrl(id, request.getImageUrl()));
    }
}