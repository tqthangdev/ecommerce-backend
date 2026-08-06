package com.dev.ecommerce.entity;

import com.dev.ecommerce.common.BaseEntity;
import com.dev.ecommerce.entity.enums.PromotionDiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "promotions")
public class Promotion extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private PromotionDiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "promotion_variants",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "variant_id")
    )
    private Set<ProductVariant> variants = new HashSet<>();

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endDate) || !active;
    }

    public boolean isNotStarted() {
        return LocalDateTime.now().isBefore(startDate);
    }

    /**
     * Computes the discounted price for a base price.
     */
    public BigDecimal applyTo(BigDecimal basePrice) {
        BigDecimal effective;
        if (discountType == PromotionDiscountType.PERCENTAGE) {
            BigDecimal discount = basePrice.multiply(
                    discountValue.divide(BigDecimal.valueOf(100))
            );
            if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                discount = maxDiscountAmount;
            }
            effective = basePrice.subtract(discount);
        } else {
            effective = basePrice.subtract(discountValue);
        }
        return effective.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
