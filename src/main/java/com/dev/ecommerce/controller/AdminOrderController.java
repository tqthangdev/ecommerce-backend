package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.request.OrderStatusUpdateRequest;
import com.dev.ecommerce.dto.response.OrderResponse;
import com.dev.ecommerce.dto.response.PageResponse;
import com.dev.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
@Tag(name = "Admin - Orders", description = "Admin order management")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List all orders (admin)")
    public ApiResponse<PageResponse<OrderResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success("Orders fetched", orderService.getAdminOrders(pageable, status, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order detail (admin)")
    public ApiResponse<OrderResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Order fetched", orderService.getOrderById(null, id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        return ApiResponse.success("Order status updated", orderService.updateOrderStatus(id, request));
    }
}
