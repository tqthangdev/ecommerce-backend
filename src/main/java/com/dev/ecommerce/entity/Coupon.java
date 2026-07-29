package com.dev.ecommerce.entity;

import com.dev.ecommerce.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "coupons")
public class Coupon extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "applicable_product_ids")
    private String applicableProductIds;

    @Column(name = "applicable_category_ids")
    private String applicableCategoryIds;

    public enum DiscountType {
        PERCENTAGE,
        FIXED_AMOUNT
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endDate) || !active;
    }

    public boolean isNotStarted() {
        return LocalDateTime.now().isBefore(startDate);
    }

    public boolean isExhausted() {
        return usageLimit != null && usedCount >= usageLimit;
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (discountType == DiscountType.PERCENTAGE) {
            BigDecimal discount = orderAmount.multiply(
                    discountValue.divide(BigDecimal.valueOf(100))
            );
            if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                return maxDiscountAmount;
            }
            return discount;
        } else {
            return discountValue.min(orderAmount);
        }
    }
}
