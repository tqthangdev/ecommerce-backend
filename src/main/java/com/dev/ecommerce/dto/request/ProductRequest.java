package com.dev.ecommerce.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank
    @Size(min = 2, max = 255)
    private String name;

    @Size(max = 5000)
    private String description;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal basePrice;

    @DecimalMin(value = "0.0", inclusive = true)
    @DecimalMax(value = "100.0", inclusive = true)
    private BigDecimal discountPercent;

    @PositiveOrZero
    private Integer stockQuantity;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long brandId;

    private Boolean active;
    private Boolean featured;
}