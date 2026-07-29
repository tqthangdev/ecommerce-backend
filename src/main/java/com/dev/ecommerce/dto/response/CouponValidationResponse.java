package com.dev.ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CouponValidationResponse {
    private boolean valid;
    private String code;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String message;
}
