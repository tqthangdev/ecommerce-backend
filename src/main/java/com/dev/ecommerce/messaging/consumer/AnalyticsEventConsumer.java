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
public class AnalyticsEventConsumer {

    @RabbitListener(queues = RabbitMqConfig.ORDER_ANALYTICS_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            log.info("[Analytics] Order: {} | Amount: {} VND | User: {}",
                    event.getOrderNumber(), event.getTotalAmount(), event.getUserId());
        } catch (Exception e) {
            log.error("[Analytics] Failed: {}", e.getMessage());
        }
    }
}
