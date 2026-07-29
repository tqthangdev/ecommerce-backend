package com.dev.ecommerce.mapper;

import com.dev.ecommerce.dto.request.BrandRequest;
import com.dev.ecommerce.dto.response.BrandResponse;
import com.dev.ecommerce.entity.Brand;

public final class BrandMapper {

    private BrandMapper() {
    }

    public static BrandResponse toResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .description(brand.getDescription())
                .logoUrl(brand.getLogoUrl())
                .active(brand.isActive())
                .createdAt(brand.getCreatedAt())
                .updatedAt(brand.getUpdatedAt())
                .build();
    }

    public static Brand toEntity(BrandRequest request, String slug) {
        Brand brand = new Brand();
        brand.setName(request.getName());
        brand.setSlug(slug);
        brand.setDescription(request.getDescription());
        brand.setLogoUrl(request.getLogoUrl());
        if (request.getActive() != null) {
            brand.setActive(request.getActive());
        }
        return brand;
    }

    public static void update(Brand brand, BrandRequest request, String slug) {
        brand.setName(request.getName());
        brand.setSlug(slug);
        brand.setDescription(request.getDescription());
        brand.setLogoUrl(request.getLogoUrl());
        if (request.getActive() != null) {
            brand.setActive(request.getActive());
        }
    }
}