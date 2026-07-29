package com.dev.ecommerce.messaging.consumer;

import com.dev.ecommerce.config.RabbitMqConfig;
import com.dev.ecommerce.messaging.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminNotificationConsumer {

    @RabbitListener(queues = RabbitMqConfig.ORDER_ADMIN_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            log.info("[Admin] New order: {} | {} VND | {}",
                    event.getOrderNumber(), event.getTotalAmount(), event.getUserEmail());
        } catch (Exception e) {
            log.error("[Admin] Failed: {}", e.getMessage());
        }
    }
}
