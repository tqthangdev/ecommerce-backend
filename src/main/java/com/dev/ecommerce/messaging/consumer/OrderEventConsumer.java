package com.dev.ecommerce.messaging.consumer;

import com.dev.ecommerce.config.RabbitMqConfig;
import com.dev.ecommerce.messaging.event.OrderCreatedEvent;
import com.dev.ecommerce.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMqConfig.ORDER_EMAIL_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        try {
            log.info("Processing OrderCreatedEvent for order: {}, user: {}",
                    event.getOrderNumber(), event.getUserEmail());

            String subject = "Order Confirmation - " + event.getOrderNumber();
            String body = buildOrderEmailBody(event);
            emailService.sendEmail(event.getUserEmail(), subject, body);

            log.info("Order confirmation email sent for order: {}", event.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send order email for {}: {}", event.getOrderNumber(), e.getMessage());
        }
    }

    private String buildOrderEmailBody(OrderCreatedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(event.getUserFullName()).append(",\n\n");
        sb.append("Thank you for your order!\n\n");
        sb.append("Order Number: ").append(event.getOrderNumber()).append("\n");
        sb.append("Total Amount: ").append(event.getTotalAmount()).append(" VND\n");
        sb.append("Payment Method: ").append(event.getPaymentMethod()).append("\n\n");
        sb.append("Items:\n");
        if (event.getItems() != null) {
            for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
                sb.append("  - ").append(item.getProductName())
                        .append(" x").append(item.getQuantity())
                        .append(" = ").append(item.getPrice()).append(" VND\n");
            }
        }
        sb.append("\nWe'll notify you when your order is shipped.\n\nBest regards,\nE-Commerce Team");
        return sb.toString();
    }
}
