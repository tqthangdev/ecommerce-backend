package com.dev.ecommerce.service;

import com.dev.ecommerce.config.CacheConfig;
import com.dev.ecommerce.dto.request.ApplyCouponRequest;
import com.dev.ecommerce.dto.request.CouponRequest;
import com.dev.ecommerce.dto.response.CouponResponse;
import com.dev.ecommerce.dto.response.CouponValidationResponse;
import com.dev.ecommerce.entity.Coupon;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.repository.CouponRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_COUPONS, allEntries = true)
    public CouponResponse create(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new BusinessException("Coupon code already exists: " + request.getCode(), HttpStatus.CONFLICT);
        }
        Coupon coupon = mapToEntity(request, new Coupon());
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_COUPONS, allEntries = true)
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", id));

        String newCode = request.getCode().toUpperCase();
        if (!newCode.equals(coupon.getCode()) && couponRepository.existsByCode(newCode)) {
            throw new BusinessException("Coupon code already exists: " + newCode, HttpStatus.CONFLICT);
        }

        mapToEntity(request, coupon);
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_COUPONS, allEntries = true)
    public void delete(Long id) {
        if (!couponRepository.existsById(id)) {
            throw new ResourceNotFoundException("Coupon", id);
        }
        couponRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public CouponResponse getById(Long id) {
        return toResponse(couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", id)));
    }

    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.CACHE_COUPONS)
    public List<CouponResponse> listActive() {
        return couponRepository.findAllActive(LocalDateTime.now())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CouponValidationResponse validateAndCalculate(ApplyCouponRequest request) {
        Coupon coupon = couponRepository.findByCode(request.getCode().toUpperCase())
                .orElse(null);

        if (coupon == null) {
            return invalid("Coupon not found");
        }

        if (!coupon.isActive()) {
            return invalid("Coupon is disabled");
        }

        if (coupon.isNotStarted()) {
            return invalid("Coupon is not yet active");
        }

        if (coupon.isExpired()) {
            return invalid("Coupon has expired");
        }

        if (coupon.isExhausted()) {
            return invalid("Coupon usage limit reached");
        }

        if (coupon.getMinOrderAmount() != null &&
                request.getOrderAmount().compareTo(coupon.getMinOrderAmount()) < 0) {
            return invalid("Minimum order amount is " + coupon.getMinOrderAmount());
        }

        BigDecimal discount = coupon.calculateDiscount(request.getOrderAmount());
        BigDecimal finalAmount = request.getOrderAmount().subtract(discount);

        return CouponValidationResponse.builder()
                .valid(true)
                .code(coupon.getCode())
                .discountAmount(discount)
                .finalAmount(finalAmount.max(BigDecimal.ZERO))
                .message("Coupon applied successfully")
                .build();
    }

    @Transactional
    public void useCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", couponId));
        couponRepository.incrementUsedCount(couponId);
    }

    @Transactional
    public void incrementUsage(Long id) {
        int updated = couponRepository.incrementUsedCount(id);
        if (updated == 0) {
            throw new BusinessException("Coupon usage limit reached", HttpStatus.BAD_REQUEST);
        }
    }

    private Coupon mapToEntity(CouponRequest request, Coupon coupon) {
        coupon.setCode(request.getCode().toUpperCase());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setPerUserLimit(request.getPerUserLimit());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());
        coupon.setActive(request.getActive() == null || request.getActive());
        coupon.setApplicableProductIds(joinIds(request.getApplicableProductIds()));
        coupon.setApplicableCategoryIds(joinIds(request.getApplicableCategoryIds()));
        return coupon;
    }

    private CouponResponse toResponse(Coupon coupon) {
        Long remaining = coupon.getUsageLimit() != null
                ? (long) (coupon.getUsageLimit() - coupon.getUsedCount())
                : null;

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .perUserLimit(coupon.getPerUserLimit())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .active(coupon.isActive())
                .expired(coupon.isExpired())
                .remainingUses(remaining)
                .build();
    }

    private CouponValidationResponse invalid(String message) {
        return CouponValidationResponse.builder()
                .valid(false)
                .message(message)
                .build();
    }

    private String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }
}
