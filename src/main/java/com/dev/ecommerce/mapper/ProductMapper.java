package com.dev.ecommerce.mapper;

import com.dev.ecommerce.dto.response.BrandResponse;
import com.dev.ecommerce.dto.response.CategoryResponse;
import com.dev.ecommerce.dto.response.ProductImageResponse;
import com.dev.ecommerce.dto.response.ProductResponse;
import com.dev.ecommerce.dto.response.ProductVariantResponse;
import com.dev.ecommerce.entity.Product;
import com.dev.ecommerce.entity.ProductImage;
import com.dev.ecommerce.entity.ProductVariant;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        if (product == null) return null;
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .effectivePrice(product.getEffectivePrice())
                .discountPercent(product.getDiscountPercent())
                .stockQuantity(product.getStockQuantity())
                .active(product.isActive())
                .featured(product.isFeatured())
                .viewCount(product.getViewCount())
                .category(product.getCategory() != null ? CategoryMapper.toResponse(product.getCategory()) : null)
                .brand(product.getBrand() != null ? BrandMapper.toResponse(product.getBrand()) : null)
                .variants(toVariantResponses(product.getVariants()))
                .images(toImageResponses(product.getImages()))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public static ProductVariantResponse toResponse(ProductVariant variant) {
        if (variant == null) return null;
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .color(variant.getColor())
                .size(variant.getSize())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .imageUrl(variant.getImageUrl())
                .build();
    }

    public static ProductImageResponse toResponse(ProductImage image) {
        if (image == null) return null;
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .altText(image.getAltText())
                .displayOrder(image.getDisplayOrder())
                .primary(image.isPrimary())
                .build();
    }

    private static List<ProductVariantResponse> toVariantResponses(Set<ProductVariant> variants) {
        if (variants == null) return List.of();
        return variants.stream().map(ProductMapper::toResponse).toList();
    }

    private static List<ProductImageResponse> toImageResponses(Set<ProductImage> images) {
        if (images == null) return List.of();
        return images.stream().sorted(Comparator.comparingInt(ProductImage::getDisplayOrder)).map(ProductMapper::toResponse).toList();
    }
}