package com.dev.ecommerce.entity;

import com.dev.ecommerce.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee", nullable = false, precision = 14, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        PROCESSING,
        SHIPPING,
        DELIVERED,
        CANCELLED,
        RETURNED
    }

    public enum PaymentMethod {
        COD,
        CREDIT_CARD,
        BANK_TRANSFER,
        WALLET
    }

    public enum PaymentStatus {
        UNPAID,
        PAID,
        REFUNDED,
        FAILED
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public boolean isCancellable() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    public BigDecimal calculateTotal() {
        BigDecimal sub = subtotal != null ? subtotal : BigDecimal.ZERO;
        BigDecimal ship = shippingFee != null ? shippingFee : BigDecimal.ZERO;
        BigDecimal disc = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        return sub.add(ship).subtract(disc);
    }
}
