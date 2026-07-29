package com.dev.ecommerce.dto.request;

import com.dev.ecommerce.entity.Order.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotNull
    private Long addressId;

    private Long couponId;

    private String couponCode;

    @NotNull
    private PaymentMethod paymentMethod;

    private String notes;
}
