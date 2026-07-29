package com.dev.ecommerce.service;

import com.dev.ecommerce.dto.request.ApplyCouponRequest;
import com.dev.ecommerce.dto.request.CheckoutRequest;
import com.dev.ecommerce.dto.request.OrderStatusUpdateRequest;
import com.dev.ecommerce.dto.response.*;
import com.dev.ecommerce.entity.*;
import com.dev.ecommerce.entity.Order.OrderStatus;
import com.dev.ecommerce.entity.Order.PaymentStatus;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.messaging.event.OrderCreatedEvent;
import com.dev.ecommerce.messaging.producer.OrderEventProducer;
import com.dev.ecommerce.repository.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CartRedisService cartRedisService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final CouponService couponService;
    private final JsonMapper jsonMapper;
    private final OrderEventProducer orderEventProducer;
    private final UserRepository userRepository;

    @Transactional
    public CheckoutResponse checkout(Long userId, CheckoutRequest request, String idempotencyKey) {
        // 1. Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Order existingOrder = existing.get();
                return buildCheckoutResponse(existingOrder, null);
            }
        }

        // 2. Fetch cart
        CartResponse cart = cartRedisService.getCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Cart is empty", HttpStatus.BAD_REQUEST);
        }

        // 3. Validate stock for all items
        for (CartItemResponse item : cart.getItems()) {
            inventoryService.validateStock(item.getProductId(), item.getVariantId(), item.getQuantity());
        }

        // 4. Fetch address
        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", request.getAddressId()));

        // 5. Calculate totals
        BigDecimal subtotal = cart.getSubtotal();
        BigDecimal shippingFee = calculateShippingFee(subtotal);
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            ApplyCouponRequest couponReq = new ApplyCouponRequest();
            couponReq.setCode(request.getCouponCode());
            couponReq.setOrderAmount(subtotal);
            CouponValidationResponse validation = couponService.validateAndCalculate(couponReq);
            if (!validation.isValid()) {
                throw new BusinessException(validation.getMessage(), HttpStatus.BAD_REQUEST);
            }
            discountAmount = validation.getDiscountAmount();
        }

        BigDecimal total = subtotal.add(shippingFee).subtract(discountAmount);

        // 6. Create order
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(generateOrderNumber());
        order.setShippingAddress(address);
        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(total);
        order.setCouponCode(request.getCouponCode());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setNotes(request.getNotes());
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(OrderStatus.PENDING);

        // 7. Create order items & deduct stock
        for (CartItemResponse cartItem : cart.getItems()) {
            BigDecimal itemPrice = cartItem.getEffectivePrice();
            BigDecimal itemSubtotal = itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = new OrderItem(
                    cartItem.getProductId(),
                    cartItem.getProductName(),
                    cartItem.getProductSlug(),
                    cartItem.getVariantId(),
                    cartItem.getVariantSku(),
                    cartItem.getColor(),
                    cartItem.getSize(),
                    cartItem.getQuantity(),
                    cartItem.getUnitPrice(),
                    itemPrice
            );
            order.addItem(orderItem);

            // Deduct stock with pessimistic lock
            inventoryService.deductStock(cartItem.getProductId(), cartItem.getVariantId(), cartItem.getQuantity());
        }

        Order savedOrder = orderRepository.save(order);

        // 8. Process payment
        String paymentUrl = null;
        if (request.getPaymentMethod() != Order.PaymentMethod.COD) {
            paymentUrl = paymentService.createPaymentSession(savedOrder);
        } else {
            savedOrder.setPaymentStatus(PaymentStatus.PAID);
            savedOrder.setPaymentReference("COD-" + savedOrder.getOrderNumber());
            orderRepository.save(savedOrder);
        }

        // 9. Increment coupon usage
        if (request.getCouponId() != null) {
            try {
                couponService.incrementUsage(request.getCouponId());
            } catch (Exception e) {
                log.warn("Failed to increment coupon usage: {}", e.getMessage());
            }
        }

        // 10. Save idempotency record
        if (idempotencyKey != null) {
            saveIdempotencyRecord(idempotencyKey, userId, savedOrder);
        }

        // 11. Clear cart
        cartRedisService.clearCart(userId);

        // 12. Publish OrderCreatedEvent for async processing (email, analytics, admin notification)
        publishOrderCreatedEvent(savedOrder, userId);

        log.info("Order {} created for user {}", savedOrder.getOrderNumber(), userId);
        return buildCheckoutResponse(savedOrder, paymentUrl);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId, org.springframework.data.domain.Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        OrderStatus newStatus = request.getStatus();
        validateStatusTransition(order.getStatus(), newStatus);

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);

        // Restore stock if cancelled
        if (newStatus == OrderStatus.CANCELLED &&
                (oldStatus == OrderStatus.PENDING || oldStatus == OrderStatus.CONFIRMED)) {
            for (OrderItem item : order.getItems()) {
                inventoryService.restoreStock(item.getProductId(), item.getVariantId(), item.getQuantity());
            }
            // Refund if paid
            if (order.getPaymentStatus() == PaymentStatus.PAID && order.getPaymentReference() != null) {
                paymentService.processRefund(order.getPaymentReference(), order.getTotalAmount());
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                orderRepository.save(order);
            }
        }

        log.info("Order {} status updated: {} -> {}", order.getOrderNumber(), oldStatus, newStatus);
        return toOrderResponse(order);
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        boolean valid = switch (from) {
            case PENDING -> to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED;
            case CONFIRMED -> to == OrderStatus.PROCESSING || to == OrderStatus.CANCELLED;
            case PROCESSING -> to == OrderStatus.SHIPPING;
            case SHIPPING -> to == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED, RETURNED -> false;
        };
        if (!valid) {
            throw new BusinessException(
                    "Invalid status transition: " + from + " -> " + to,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + timestamp + "-" + uuid;
    }

    private BigDecimal calculateShippingFee(BigDecimal subtotal) {
        if (subtotal.compareTo(BigDecimal.valueOf(500000)) >= 0) {
            return BigDecimal.ZERO; // Free shipping for orders >= 500k
        }
        return BigDecimal.valueOf(30000); // 30k shipping fee
    }

    private CheckoutResponse buildCheckoutResponse(Order order, String paymentUrl) {
        return CheckoutResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .paymentUrl(paymentUrl)
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .build();
    }

    private void saveIdempotencyRecord(String key, Long userId, Order order) {
        try {
            CheckoutResponse resp = buildCheckoutResponse(order, null);
            String body = jsonMapper.writeValueAsString(resp);
            LocalDateTime expires = LocalDateTime.now().plusHours(24);
            IdempotencyRecord record = new IdempotencyRecord(
                    key, userId, "/api/orders/checkout", body, 201, expires
            );
            idempotencyRepository.save(record);
        } catch (JacksonException e) {
            log.warn("Failed to save idempotency record: {}", e.getMessage());
        }
    }

    private void publishOrderCreatedEvent(Order order, Long userId) {
        try {
            var user = userRepository.findById(userId).orElse(null);
            String email = user != null ? user.getEmail() : "";
            String fullName = user != null ? user.getFullName() : "";

            List<OrderCreatedEvent.OrderItemEvent> itemEvents = order.getItems().stream()
                    .map(item -> OrderCreatedEvent.OrderItemEvent.builder()
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .quantity(item.getQuantity())
                            .price(item.getEffectivePrice())
                            .build())
                    .toList();

            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .userId(userId)
                    .userEmail(email)
                    .userFullName(fullName)
                    .totalAmount(order.getTotalAmount())
                    .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                    .items(itemEvents)
                    .createdAt(order.getCreatedAt())
                    .build();

            orderEventProducer.publishOrderCreated(event);
        } catch (Exception e) {
            log.warn("Failed to publish OrderCreatedEvent: {}", e.getMessage());
        }
    }

    private OrderResponse toOrderResponse(Order order) {
        Address addr = order.getShippingAddress();
        AddressResponse addrResp = addr != null ? AddressResponse.builder()
                .id(addr.getId())
                .recipientName(addr.getRecipientName())
                .phone(addr.getPhone())
                .fullAddress(addr.getStreetAddress() + ", " + addr.getWardName() + ", " +
                        addr.getDistrictName() + ", " + addr.getProvinceName())
                .defaultAddress(addr.isDefaultAddress())
                .build() : null;

        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productSlug(item.getProductSlug())
                        .productImageUrl(item.getProductImageUrl())
                        .variantId(item.getVariantId())
                        .variantSku(item.getVariantSku())
                        .variantColor(item.getVariantColor())
                        .variantSize(item.getVariantSize())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .effectivePrice(item.getEffectivePrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentReference(order.getPaymentReference())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .couponCode(order.getCouponCode())
                .shippingAddress(addrResp)
                .items(items)
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .cancellable(order.isCancellable())
                .build();
    }
}
