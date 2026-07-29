package com.dev.ecommerce.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";

    // Queues
    public static final String ORDER_EMAIL_QUEUE = "order.email.queue";
    public static final String ORDER_ANALYTICS_QUEUE = "order.analytics.queue";
    public static final String ORDER_ADMIN_QUEUE = "order.admin.queue";

    // Routing keys
    public static final String ORDER_CREATED_KEY = "order.created";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue orderEmailQueue() {
        return QueueBuilder.durable(ORDER_EMAIL_QUEUE).build();
    }

    @Bean
    public Queue orderAnalyticsQueue() {
        return QueueBuilder.durable(ORDER_ANALYTICS_QUEUE).build();
    }

    @Bean
    public Queue orderAdminQueue() {
        return QueueBuilder.durable(ORDER_ADMIN_QUEUE).build();
    }

    @Bean
    public Binding orderEmailBinding(Queue orderEmailQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderEmailQueue).to(orderExchange).with(ORDER_CREATED_KEY);
    }

    @Bean
    public Binding orderAnalyticsBinding(Queue orderAnalyticsQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderAnalyticsQueue).to(orderExchange).with(ORDER_CREATED_KEY);
    }

    @Bean
    public Binding orderAdminBinding(Queue orderAdminQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderAdminQueue).to(orderExchange).with(ORDER_CREATED_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
