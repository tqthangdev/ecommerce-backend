package com.dev.ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponse {
    private Long productId;
    private Long variantId;
    private String productName;
    private String productSlug;
    private String variantSku;
    private String color;
    private String size;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal effectivePrice;
    private BigDecimal subtotal;
    private int stockAvailable;
}
