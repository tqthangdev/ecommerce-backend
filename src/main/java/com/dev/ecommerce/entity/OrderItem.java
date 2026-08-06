package com.dev.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_slug")
    private String productSlug;

    @Column(name = "product_image_url")
    private String productImageUrl;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "variant_name", length = 255)
    private String variantName;

    @Column(name = "variant_sku", length = 80)
    private String variantSku;

    @Column(name = "variant_color", length = 50)
    private String variantColor;

    @Column(name = "variant_size", length = 50)
    private String variantSize;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "effective_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal effectivePrice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    public OrderItem(Long productId, String productName, String productSlug,
                     Long variantId, String variantName, String variantSku,
                     String variantColor, String variantSize, int quantity,
                     BigDecimal unitPrice, BigDecimal effectivePrice) {
        this.productId = productId;
        this.productName = productName;
        this.productSlug = productSlug;
        this.variantId = variantId;
        this.variantName = variantName;
        this.variantSku = variantSku;
        this.variantColor = variantColor;
        this.variantSize = variantSize;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.effectivePrice = effectivePrice;
        this.subtotal = effectivePrice.multiply(BigDecimal.valueOf(quantity));
    }
}
