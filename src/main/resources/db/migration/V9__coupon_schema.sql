-- Sprint 4: Coupon schema

CREATE TABLE coupons (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                   VARCHAR(50) NOT NULL UNIQUE,
    description            VARCHAR(255) NOT NULL,
    discount_type          VARCHAR(20) NOT NULL,
    discount_value         DECIMAL(12,2) NOT NULL,
    min_order_amount       DECIMAL(12,2),
    max_discount_amount    DECIMAL(12,2),
    usage_limit            INT,
    used_count             INT NOT NULL DEFAULT 0,
    per_user_limit         INT,
    start_date             DATETIME NOT NULL,
    end_date               DATETIME NOT NULL,
    is_active              BOOLEAN NOT NULL DEFAULT TRUE,
    applicable_product_ids TEXT,
    applicable_category_ids TEXT,
    created_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_coupons_code (code),
    INDEX idx_coupons_active (is_active, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
