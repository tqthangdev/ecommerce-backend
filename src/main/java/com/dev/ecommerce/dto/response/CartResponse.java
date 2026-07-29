package com.dev.ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private List<CartItemResponse> items;
    private int totalItems;
    private int totalQuantity;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
}
