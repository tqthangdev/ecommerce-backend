package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.request.CheckoutRequest;
import com.dev.ecommerce.dto.response.CheckoutResponse;
import com.dev.ecommerce.dto.response.OrderResponse;
import com.dev.ecommerce.dto.response.PageResponse;
import com.dev.ecommerce.security.UserPrincipal;
import com.dev.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement and management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place an order (checkout)")
    public ApiResponse<CheckoutResponse> checkout(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        CheckoutResponse response = orderService.checkout(principal.getId(), request, idempotencyKey);
        return ApiResponse.success("Order placed", response);
    }

    @GetMapping
    @Operation(summary = "List user orders")
    public ApiResponse<PageResponse<OrderResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success("Orders fetched", orderService.getUserOrderPage(principal.getId(), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by id")
    public ApiResponse<OrderResponse> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.success("Order fetched", orderService.getOrderById(principal.getId(), id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order")
    public ApiResponse<OrderResponse> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.success("Order cancelled", orderService.cancelOrder(principal.getId(), id));
    }
}
