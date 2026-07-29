package com.dev.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApplyCouponRequest {

    @NotBlank
    private String code;

    @NotNull
    @Positive
    private BigDecimal orderAmount;
}
