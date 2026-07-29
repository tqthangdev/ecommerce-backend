package com.dev.ecommerce.dto.request;

import com.dev.ecommerce.entity.Coupon.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CouponRequest {

    @NotBlank
    @Size(min = 4, max = 50)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String description;

    @NotNull
    private DiscountType discountType;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.00")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.01")
    private BigDecimal maxDiscountAmount;

    @Min(1)
    private Integer usageLimit;

    @Min(1)
    private Integer perUserLimit;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    @Future
    private LocalDateTime endDate;

    private Boolean active;

    private List<Long> applicableProductIds;

    private List<Long> applicableCategoryIds;
}
