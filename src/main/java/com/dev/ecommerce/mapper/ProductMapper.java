package com.dev.ecommerce.mapper;

import com.dev.ecommerce.dto.response.BrandResponse;
import com.dev.ecommerce.dto.response.CategoryResponse;
import com.dev.ecommerce.dto.response.ProductImageResponse;
import com.dev.ecommerce.dto.response.ProductResponse;
import com.dev.ecommerce.dto.response.ProductVariantResponse;
import com.dev.ecommerce.entity.Product;
import com.dev.ecommerce.entity.ProductImage;
import com.dev.ecommerce.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        return toResponse(product, null);
    }

    /**
     * Maps a product to its response. When {@code priceResolver} is provided, it is
     * used to compute the promotion-applied sale price for each active variant, and
     * the lowest one is exposed as {@code salePrice} on the response.
     */
    public static ProductResponse toResponse(Product product, java.util.function.Function<ProductVariant, BigDecimal> priceResolver) {
        if (product == null) return null;
        List<ProductVariantResponse> variants = toVariantResponses(product.getVariants());
        List<BigDecimal> prices = variants.stream()
                .filter(ProductVariantResponse::isActive)
                .map(ProductVariantResponse::getPrice)
                .sorted()
                .toList();

        BigDecimal minPrice = prices.isEmpty() ? null : prices.get(0);
        BigDecimal maxPrice = prices.isEmpty() ? null : prices.get(prices.size() - 1);

        BigDecimal salePrice = null;
        if (priceResolver != null) {
            salePrice = product.getVariants().stream()
                    .filter(ProductVariant::isActive)
                    .map(priceResolver)
                    .min(BigDecimal::compareTo)
                    .orElse(null);
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .salePrice(salePrice)
                .active(product.isActive())
                .featured(product.isFeatured())
                .viewCount(product.getViewCount())
                .category(product.getCategory() != null ? CategoryMapper.toResponse(product.getCategory()) : null)
                .brand(product.getBrand() != null ? BrandMapper.toResponse(product.getBrand()) : null)
                .variants(variants)
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
                .active(variant.isActive())
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