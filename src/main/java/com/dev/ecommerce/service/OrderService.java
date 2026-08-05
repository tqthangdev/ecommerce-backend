package com.dev.ecommerce.service;

import com.dev.ecommerce.dto.request.CheckoutRequest;
import com.dev.ecommerce.dto.request.OrderStatusUpdateRequest;
import com.dev.ecommerce.dto.response.*;
import com.dev.ecommerce.entity.*;
import com.dev.ecommerce.entity.Order.OrderStatus;
import com.dev.ecommerce.entity.Order.PaymentStatus;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.messaging.event.OrderCreatedEvent;
import com.dev.ecommerce.messaging.producer.OrderEventProducer;
import com.dev.ecommerce.repository.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final CartRedisService cartRedisService;
    private final PaymentService paymentService;
    private final CouponService couponService;
    private final JsonMapper jsonMapper;
    private final OrderEventProducer orderEventProducer;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    // All @Transactional DB work lives here — see OrderTransactionalOps javadoc for why
    // it must be a separate bean rather than protected methods on this class.
    private final OrderTransactionalOps orderTx;

    /**
     * Public entry point. NOT @Transactional at this level on purpose:
     * step 1 (create order + deduct stock) commits in its own short transaction
     * (inside orderTx, a different bean — so its @Transactional actually applies),
     * then the payment gateway call happens OUTSIDE any DB transaction so we never
     * hold row locks (from deductStock) while waiting on a network call to
     * VNPay/Momo/Stripe/etc. Step 3 persists the payment result in a second short
     * transaction.
     */
    public CheckoutResponse checkout(Long userId, CheckoutRequest request, String idempotencyKey) {
        // 0. Idempotency check (best-effort fast path; the authoritative check is the
        // unique constraint on idempotency_key, handled in orderTx.createOrderAndDeductStock).
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return buildCheckoutResponse(existing.get(), null);
            }
        }

        // 1. Create order + deduct stock, all in one short DB transaction.
        Order savedOrder = orderTx.createOrderAndDeductStock(userId, request, idempotencyKey);

        // 2. Process payment OUTSIDE the DB transaction — no locks held during the
        // external call.
        String paymentUrl = null;
        if (request.getPaymentMethod() != Order.PaymentMethod.COD) {
            paymentUrl = paymentService.createPaymentSession(savedOrder);
        } else {
            orderTx.markOrderPaidCod(savedOrder.getId());
        }

        // 3. Increment coupon usage (best-effort, does not affect order success)
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            try {
                Coupon coupon = couponRepository.findByCode(request.getCouponCode())
                        .orElseThrow(() -> new ResourceNotFoundException("Coupon", request.getCouponCode()));

                couponService.incrementUsage(coupon.getId());
            } catch (Exception e) {
                log.warn("Failed to increment coupon usage for code {}: {}",
                        request.getCouponCode(), e.getMessage());
            }
        }

        // 4. Save idempotency record
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            saveIdempotencyRecord(idempotencyKey, userId, savedOrder);
        }

        // 5. Clear cart
        cartRedisService.clearCart(userId);

        // 6. Publish OrderCreatedEvent for async processing (email, analytics, admin notification)
        publishOrderCreatedEvent(savedOrder, userId);

        log.info("Order {} created for user {}", savedOrder.getOrderNumber(), userId);
        return buildCheckoutResponse(savedOrder, paymentUrl);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId, org.springframework.data.domain.Pageable pageable) {
        if (userId == null) {
            return orderRepository.findAllOrdersForAdmin(pageable)
                    .stream()
                    .map(this::toOrderResponse)
                    .collect(Collectors.toList());
        }
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getUserOrderPage(Long userId, org.springframework.data.domain.Pageable pageable) {
        var page = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<OrderResponse> content = page.getContent().stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());

        return PageResponse.<OrderResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAdminOrders(org.springframework.data.domain.Pageable pageable, String status, String keyword) {
        var page = orderRepository.findAllOrdersForAdminWithFilter(
                status != null && !status.isBlank() ? OrderStatus.valueOf(status) : null,
                keyword != null && !keyword.isBlank() ? keyword : null,
                pageable
        );
        List<OrderResponse> content = page.getContent().stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());

        return PageResponse.<OrderResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId) {
        Order order;
        if (userId == null) {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        } else {
            order = orderRepository.findByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        }
        return toOrderResponse(order);
    }

    /**
     * Status update WITHOUT refund still needs to stay transactional with the stock
     * restore (handled inside orderTx.applyStatusTransition). The refund call to the
     * payment gateway is done AFTER commit so we don't hold DB locks during that
     * network call; if the refund fails we log and leave the order as PAID for manual
     * reconciliation.
     */
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        OrderStatus[] transition = new OrderStatus[2]; // [old, new]
        Order order = orderTx.applyStatusTransition(orderId, request.getStatus(), transition);

        if (transition[1] == OrderStatus.CANCELLED &&
                (transition[0] == OrderStatus.PENDING || transition[0] == OrderStatus.CONFIRMED)) {
            if (order.getPaymentStatus() == PaymentStatus.PAID && order.getPaymentReference() != null) {
                try {
                    paymentService.processRefund(order.getPaymentReference(), order.getTotalAmount());
                    orderTx.markOrderRefunded(order.getId());
                } catch (Exception e) {
                    log.error("Refund failed for order {}: {}. Order left as PAID for manual reconciliation.",
                            order.getOrderNumber(), e.getMessage());
                }
            }
        }

        log.info("Order {} status updated: {} -> {}", order.getOrderNumber(), transition[0], transition[1]);
        return toOrderResponse(order);
    }

    /**
     * Same pattern: DB work (cancellability check + stock restore, inside orderTx.doCancel)
     * commits first; refund happens after, outside the transaction.
     */
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderTx.doCancel(userId, orderId);

        if (order.getPaymentStatus() == PaymentStatus.PAID
                && order.getPaymentMethod() != Order.PaymentMethod.COD) {
            try {
                paymentService.refund(order.getPaymentReference());
                orderTx.markOrderRefunded(order.getId());
            } catch (Exception e) {
                log.error("Refund failed for order {}: {}. Order left as PAID for manual reconciliation.",
                        order.getOrderNumber(), e.getMessage());
            }
        }

        log.info("Order {} cancelled by user {}", order.getOrderNumber(), userId);
        return toOrderResponse(order);
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