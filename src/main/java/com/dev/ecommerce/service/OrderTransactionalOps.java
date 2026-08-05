package com.dev.ecommerce.service;

import com.dev.ecommerce.dto.request.ApplyCouponRequest;
import com.dev.ecommerce.dto.request.CheckoutRequest;
import com.dev.ecommerce.dto.response.CartResponse;
import com.dev.ecommerce.dto.response.CartItemResponse;
import com.dev.ecommerce.dto.response.CouponValidationResponse;
import com.dev.ecommerce.entity.Address;
import com.dev.ecommerce.entity.Order;
import com.dev.ecommerce.entity.Order.OrderStatus;
import com.dev.ecommerce.entity.Order.PaymentStatus;
import com.dev.ecommerce.entity.OrderItem;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.repository.AddressRepository;
import com.dev.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Holds every @Transactional write operation used by order checkout / status
 * changes / cancellation.
 *
 * WHY THIS IS A SEPARATE BEAN: Spring's @Transactional (and @Cacheable, @CacheEvict,
 * etc.) is implemented via a proxy wrapped around the bean. If a method on a bean
 * calls ANOTHER @Transactional method on *itself* (e.g. via a plain `this.foo()` or
 * an unqualified call from within the same class), that call bypasses the proxy
 * entirely — the annotation is silently ignored, no transaction is actually started.
 * This is a well-known Spring AOP pitfall ("self-invocation").
 *
 * OrderService's public methods (checkout, updateOrderStatus, cancelOrder) are
 * intentionally NOT @Transactional themselves (see OrderService javadoc — we don't
 * want to hold DB locks while calling external payment gateways). They need to
 * delegate the DB-only portions of the work to genuinely separate transactions.
 * Putting those portions here, in a different bean, means OrderService calls them
 * through a real Spring-managed proxy, so @Transactional actually applies.
 */
@Service
@RequiredArgsConstructor
class OrderTransactionalOps {

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final CartRedisService cartRedisService;
    private final InventoryService inventoryService;
    private final CouponService couponService;

    /**
     * Creates the order and deducts stock for every line item atomically.
     * Stock rows are locked in a deterministic order (see InventoryService.deductStockBatch)
     * to avoid deadlocks between concurrent checkouts sharing products.
     * If the idempotency key already exists (race with a concurrent duplicate request),
     * the unique constraint on idempotency_key will fail this transaction and we return
     * the row the winning request created instead of erroring.
     */
    @Transactional
    Order createOrderAndDeductStock(Long userId, CheckoutRequest request, String idempotencyKey) {
        try {
            CartResponse cart = cartRedisService.getCart(userId);
            if (cart.getItems().isEmpty()) {
                throw new BusinessException("Cart is empty", HttpStatus.BAD_REQUEST);
            }

            Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Address", request.getAddressId()));

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

            for (CartItemResponse cartItem : cart.getItems()) {
                BigDecimal itemPrice = cartItem.getEffectivePrice();
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
            }

            List<InventoryService.StockLine> lines = cart.getItems().stream()
                    .map(ci -> new InventoryService.StockLine(ci.getProductId(), ci.getVariantId(), ci.getQuantity()))
                    .toList();
            inventoryService.deductStockBatch(lines);

            return orderRepository.save(order);

        } catch (DataIntegrityViolationException e) {
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                return orderRepository.findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(() -> e);
            }
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void markOrderPaidCod(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentReference("COD-" + order.getOrderNumber());
        orderRepository.save(order);
    }

    @Transactional
    Order applyStatusTransition(Long orderId, OrderStatus newStatus, OrderStatus[] outOldNew) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        validateStatusTransition(order.getStatus(), newStatus);

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);

        if (newStatus == OrderStatus.CANCELLED &&
                (oldStatus == OrderStatus.PENDING || oldStatus == OrderStatus.CONFIRMED)) {
            for (OrderItem item : order.getItems()) {
                inventoryService.restoreStock(item.getProductId(), item.getVariantId(), item.getQuantity());
            }
        }

        outOldNew[0] = oldStatus;
        outOldNew[1] = newStatus;
        return order;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void markOrderRefunded(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(order);
    }

    @Transactional
    Order doCancel(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.isCancellable()) {
            throw new BusinessException(
                    "Order cannot be cancelled. Current status: " + order.getStatus(),
                    HttpStatus.BAD_REQUEST
            );
        }

        for (OrderItem item : order.getItems()) {
            inventoryService.restoreStock(item.getProductId(), item.getVariantId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        return order;
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
}