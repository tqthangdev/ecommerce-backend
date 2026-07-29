package com.dev.ecommerce.messaging.producer;

import com.dev.ecommerce.config.RabbitMqConfig;
import com.dev.ecommerce.messaging.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ORDER_EXCHANGE,
                    RabbitMqConfig.ORDER_CREATED_KEY,
                    event
            );
            log.info("OrderCreatedEvent published for order: {}", event.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to publish OrderCreatedEvent for order {}: {}",
                    event.getOrderNumber(), e.getMessage());
        }
    }
}
