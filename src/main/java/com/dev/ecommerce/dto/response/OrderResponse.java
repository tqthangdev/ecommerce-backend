package com.dev.ecommerce.dto.response;

import com.dev.ecommerce.entity.Order.OrderStatus;
import com.dev.ecommerce.entity.Order.PaymentMethod;
import com.dev.ecommerce.entity.Order.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String paymentReference;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String couponCode;
    private AddressResponse shippingAddress;
    private List<OrderItemResponse> items;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean cancellable;
}
