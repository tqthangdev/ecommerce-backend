package com.dev.ecommerce.entity;

import com.dev.ecommerce.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(length = 50)
    private String color;

    @Column(length = 50)
    private String size;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity = 0;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    public ProductVariant(Product product, String sku, String color, String size,
                          BigDecimal price, int stockQuantity, String imageUrl) {
        this.product = product;
        this.sku = sku;
        this.color = color;
        this.size = size;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imageUrl = imageUrl;
    }
}
