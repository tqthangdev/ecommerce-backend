-- Sprint 5: Order and OrderItem schema

CREATE TABLE orders (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    order_number      VARCHAR(50) NOT NULL UNIQUE,
    address_id        BIGINT,
    status            VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    subtotal          DECIMAL(14,2) NOT NULL,
    shipping_fee      DECIMAL(14,2) NOT NULL DEFAULT 0,
    discount_amount   DECIMAL(14,2) DEFAULT 0,
    total_amount      DECIMAL(14,2) NOT NULL,
    coupon_id         BIGINT,
    coupon_code       VARCHAR(50),
    payment_method    VARCHAR(30),
    payment_status    VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    payment_reference VARCHAR(255),
    idempotency_key   VARCHAR(100) UNIQUE,
    notes             TEXT,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_status (status),
    INDEX idx_orders_number (order_number),
    INDEX idx_orders_idem (idempotency_key),
    INDEX idx_orders_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id            BIGINT NOT NULL,
    product_id          BIGINT NOT NULL,
    product_name        VARCHAR(255) NOT NULL,
    product_slug        VARCHAR(280),
    product_image_url   VARCHAR(500),
    variant_id          BIGINT,
    variant_sku         VARCHAR(80),
    variant_color       VARCHAR(50),
    variant_size        VARCHAR(50),
    quantity            INT NOT NULL,
    unit_price          DECIMAL(14,2) NOT NULL,
    effective_price     DECIMAL(14,2) NOT NULL,
    subtotal            DECIMAL(14,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_items_order (order_id),
    INDEX idx_order_items_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
