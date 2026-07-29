package com.dev.ecommerce.service;

import com.dev.ecommerce.config.CacheConfig;
import com.dev.ecommerce.dto.request.CategoryRequest;
import com.dev.ecommerce.dto.response.CategoryResponse;
import com.dev.ecommerce.dto.response.PageResponse;
import com.dev.ecommerce.entity.Category;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.mapper.CategoryMapper;
import com.dev.ecommerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SlugService slugService;

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true)
    public CategoryResponse create(CategoryRequest request) {
        String baseSlug = slugService.slugify(request.getName());
        String slug = ensureUniqueSlug(baseSlug, null);
        if (categoryRepository.existsBySlug(slug)) {
            throw new BusinessException("Category slug already exists: " + slug, HttpStatus.CONFLICT);
        }
        Category category = CategoryMapper.toEntity(request, slug);
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true)
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        String baseSlug = slugService.slugify(request.getName());
        String slug = ensureUniqueSlug(baseSlug, id);
        if (!slug.equals(category.getSlug()) && categoryRepository.existsBySlug(slug)) {
            throw new BusinessException("Category slug already exists: " + slug, HttpStatus.CONFLICT);
        }
        CategoryMapper.update(category, request, slug);
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true)
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        if (!category.getProducts().isEmpty()) {
            throw new BusinessException(
                    "Cannot delete category with existing products",
                    HttpStatus.CONFLICT
            );
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.CACHE_CATEGORIES)
    public CategoryResponse getById(Long id) {
        return CategoryMapper.toResponse(categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id)));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        return CategoryMapper.toResponse(categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", slug)));
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> list(Pageable pageable) {
        Page<Category> page = categoryRepository.findAll(pageable);
        return PageResponse.from(page, CategoryMapper::toResponse);
    }

    private String ensureUniqueSlug(String base, Long excludeId) {
        String slug = base;
        int attempts = 0;
        while (categoryRepository.existsBySlug(slug)) {
            attempts++;
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 6);
            if (attempts > 5) {
                throw new BusinessException("Could not generate unique slug", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return slug;
    }
}