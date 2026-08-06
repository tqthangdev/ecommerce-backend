package com.dev.ecommerce.dto.response;

import com.dev.ecommerce.entity.enums.PromotionDiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PromotionResponse {
    private Long id;
    private String name;
    private String description;
    private PromotionDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;
    private boolean expired;
    private List<Long> variantIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
