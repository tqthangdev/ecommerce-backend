package com.dev.ecommerce.messaging.event;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderCreatedEvent implements Serializable {
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private List<OrderItemEvent> items;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class OrderItemEvent implements Serializable {
        private Long productId;
        private String productName;
        private int quantity;
        private BigDecimal price;
    }
}
