package com.dev.ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private String productImageUrl;
    private Long variantId;
    private String variantSku;
    private String variantColor;
    private String variantSize;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal effectivePrice;
    private BigDecimal subtotal;
}
