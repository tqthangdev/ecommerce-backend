package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.entity.Order;
import com.dev.ecommerce.repository.OrderRepository;
import com.dev.ecommerce.repository.ProductRepository;
import com.dev.ecommerce.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin - Dashboard", description = "Dashboard statistics")
public class DashboardController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", productRepository.count());
        stats.put("totalOrders", orderRepository.count());
        stats.put("totalUsers", userRepository.count());
        stats.put("totalRevenue", orderRepository.getTotalRevenue() != null
                ? orderRepository.getTotalRevenue() : BigDecimal.ZERO);
        return ApiResponse.success("Stats fetched", stats);
    }

    @GetMapping("/recent-orders")
    @Operation(summary = "Get recent orders")
    public ApiResponse<List<Map<String, Object>>> getRecentOrders() {
        List<Order> orders = orderRepository.findTop5ByOrderByCreatedAtDesc(PageRequest.of(0, 5));
        List<Map<String, Object>> result = orders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNumber", order.getOrderNumber());
            map.put("status", order.getStatus().name());
            map.put("totalAmount", order.getTotalAmount());
            map.put("createdAt", order.getCreatedAt());
            map.put("paymentStatus", order.getPaymentStatus().name());
            map.put("paymentMethod", order.getPaymentMethod().name());
            return map;
        }).toList();
        return ApiResponse.success("Recent orders fetched", result);
    }
}
