package com.dev.ecommerce.service;

import com.dev.ecommerce.config.CacheConfig;
import com.dev.ecommerce.dto.request.BrandRequest;
import com.dev.ecommerce.dto.response.BrandResponse;
import com.dev.ecommerce.dto.response.PageResponse;
import com.dev.ecommerce.entity.Brand;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.mapper.BrandMapper;
import com.dev.ecommerce.repository.BrandRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final SlugService slugService;

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_BRANDS, allEntries = true)
    public BrandResponse create(BrandRequest request) {
        String baseSlug = slugService.slugify(request.getName());
        String slug = ensureUniqueSlug(baseSlug, null);
        if (brandRepository.existsBySlug(slug)) {
            throw new BusinessException("Brand slug already exists: " + slug, HttpStatus.CONFLICT);
        }
        Brand brand = BrandMapper.toEntity(request, slug);
        return BrandMapper.toResponse(brandRepository.save(brand));
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_BRANDS, allEntries = true)
    public BrandResponse update(Long id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", id));
        String baseSlug = slugService.slugify(request.getName());
        String slug = ensureUniqueSlug(baseSlug, id);
        if (!slug.equals(brand.getSlug()) && brandRepository.existsBySlug(slug)) {
            throw new BusinessException("Brand slug already exists: " + slug, HttpStatus.CONFLICT);
        }
        BrandMapper.update(brand, request, slug);
        return BrandMapper.toResponse(brandRepository.save(brand));
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_BRANDS, allEntries = true)
    public void delete(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", id));
        if (!brand.getProducts().isEmpty()) {
            throw new BusinessException(
                    "Cannot delete brand with existing products",
                    HttpStatus.CONFLICT
            );
        }
        brandRepository.delete(brand);
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.CACHE_BRANDS)
    public BrandResponse getById(Long id) {
        return BrandMapper.toResponse(brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", id)));
    }

    @Transactional(readOnly = true)
    public BrandResponse getBySlug(String slug) {
        return BrandMapper.toResponse(brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", slug)));
    }

    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> list(Pageable pageable) {
        Page<Brand> page = brandRepository.findAll(pageable);
        return PageResponse.from(page, BrandMapper::toResponse);
    }

    private String ensureUniqueSlug(String base, Long excludeId) {
        String slug = base;
        int attempts = 0;
        while (brandRepository.existsBySlug(slug)) {
            attempts++;
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 6);
            if (attempts > 5) {
                throw new BusinessException("Could not generate unique slug", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return slug;
    }
}