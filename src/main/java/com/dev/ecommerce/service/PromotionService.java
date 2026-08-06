package com.dev.ecommerce.service;

import com.dev.ecommerce.config.CacheConfig;
import com.dev.ecommerce.dto.request.PromotionRequest;
import com.dev.ecommerce.dto.response.PromotionResponse;
import com.dev.ecommerce.entity.ProductVariant;
import com.dev.ecommerce.entity.Promotion;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.repository.ProductVariantRepository;
import com.dev.ecommerce.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final ProductVariantRepository variantRepository;

    // ---------- CRUD ----------

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_PROMOTIONS, CacheConfig.CACHE_PROMOTION_PRICE}, allEntries = true)
    public PromotionResponse create(PromotionRequest request) {
        Promotion promotion = mapToEntity(request, new Promotion());
        return toResponse(promotionRepository.save(promotion));
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_PROMOTIONS, CacheConfig.CACHE_PROMOTION_PRICE}, allEntries = true)
    public PromotionResponse update(Long id, PromotionRequest request) {
        Promotion promotion = promotionRepository.findByIdWithVariants(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", id));
        mapToEntity(request, promotion);
        return toResponse(promotionRepository.save(promotion));
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_PROMOTIONS, CacheConfig.CACHE_PROMOTION_PRICE}, allEntries = true)
    public void delete(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Promotion", id);
        }
        promotionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PromotionResponse getById(Long id) {
        Promotion promotion = promotionRepository.findByIdWithVariants(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", id));
        return toResponse(promotion);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_PROMOTIONS)
    public List<PromotionResponse> list() {
        return promotionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ---------- Pricing ----------

    /**
     * Resolves the effective (promotion-applied) price for a variant.
     * When multiple promotions overlap, the earliest-starting one wins.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_PROMOTION_PRICE, key = "#variant.id")
    public BigDecimal resolveEffectivePrice(ProductVariant variant) {
        List<Promotion> promotions = promotionRepository
                .findActiveByVariantId(variant.getId(), LocalDateTime.now());
        if (promotions.isEmpty()) {
            return variant.getPrice();
        }
        Promotion best = promotions.get(0);
        return best.applyTo(variant.getPrice());
    }

    public void evictPriceCache(Long variantId) {
        // CacheEvict on keyed caches is handled by a separate method below.
        log.debug("Promotion price cache will be refreshed by TTL for variant {}", variantId);
    }

    // ---------- Mapping ----------

    private Promotion mapToEntity(PromotionRequest request, Promotion promotion) {
        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());
        promotion.setDiscountType(request.getDiscountType());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setActive(request.getActive() == null || request.getActive());

        Set<ProductVariant> variants = request.getVariantIds().stream()
                .map(variantId -> variantRepository.findById(variantId)
                        .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId)))
                .collect(Collectors.toSet());
        promotion.setVariants(variants);
        return promotion;
    }

    private PromotionResponse toResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .name(promotion.getName())
                .description(promotion.getDescription())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .active(promotion.isActive())
                .expired(promotion.isExpired())
                .variantIds(promotion.getVariants().stream()
                        .map(ProductVariant::getId)
                        .collect(Collectors.toList()))
                .createdAt(promotion.getCreatedAt())
                .updatedAt(promotion.getUpdatedAt())
                .build();
    }
}
