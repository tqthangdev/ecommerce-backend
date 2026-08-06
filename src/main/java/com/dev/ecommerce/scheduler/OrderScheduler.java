package com.dev.ecommerce.scheduler;

import com.dev.ecommerce.entity.Order.OrderStatus;
import com.dev.ecommerce.entity.Order;
import com.dev.ecommerce.entity.OrderItem;
import com.dev.ecommerce.entity.IdempotencyRecord;
import com.dev.ecommerce.repository.IdempotencyRepository;
import com.dev.ecommerce.repository.OrderRepository;
import com.dev.ecommerce.service.InventoryService;
import com.dev.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final IdempotencyRepository idempotencyRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int PENDING_ORDER_TIMEOUT_MINUTES = 30;

    @Scheduled(fixedRate = 300000) // every 5 minutes
    @Transactional
    public void cancelExpiredPendingOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(PENDING_ORDER_TIMEOUT_MINUTES);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING, threshold
        );

        if (expiredOrders.isEmpty()) return;

        log.info("Found {} expired pending orders to cancel", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                // Restore stock
                for (OrderItem item : order.getItems()) {
                    inventoryService.restoreStock(
                            item.getVariantId(),
                            item.getQuantity()
                    );
                }

                // Process refund if paid
                if (order.getPaymentStatus() == com.dev.ecommerce.entity.Order.PaymentStatus.PAID
                        && order.getPaymentReference() != null) {
                    paymentService.processRefund(order.getPaymentReference(), order.getTotalAmount());
                    order.setPaymentStatus(com.dev.ecommerce.entity.Order.PaymentStatus.REFUNDED);
                }

                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);

                log.info("Order {} cancelled due to timeout (PENDING for > {} min)",
                        order.getOrderNumber(), PENDING_ORDER_TIMEOUT_MINUTES);
            } catch (Exception e) {
                log.error("Failed to cancel expired order {}: {}",
                        order.getOrderNumber(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * ?") // 3 AM daily
    @Transactional
    public void cleanupExpiredIdempotencyRecords() {
        int deleted = idempotencyRepository.deleteExpiredRecords(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency records", deleted);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // midnight daily
    public void cleanupStaleRateLimitKeys() {
        // Redis keys with TTL auto-expire, but we can log stats
        log.debug("Rate limit key TTL is handled by Redis natively");
    }
}
