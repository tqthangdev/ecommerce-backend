package com.dev.ecommerce.dto.request;

import com.dev.ecommerce.entity.Order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    @NotNull
    private OrderStatus status;
}
