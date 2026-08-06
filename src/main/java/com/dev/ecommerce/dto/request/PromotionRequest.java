package com.dev.ecommerce.dto.request;

import com.dev.ecommerce.entity.enums.PromotionDiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionRequest {

    @NotBlank(message = "Promotion name is required")
    @Size(min = 2, max = 150, message = "Promotion name must be between 2 and 150 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Discount type is required")
    private PromotionDiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be at least 0.01")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.01", message = "Maximum discount amount must be at least 0.01")
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    private Boolean active;

    @NotNull(message = "At least one variant must be selected")
    private List<Long> variantIds;
}
