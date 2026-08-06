-- Sprint: Product Schema Refactoring
-- 1. Remove sales info from products (moved to product_variants)
-- 2. Add is_active to product_variants
-- 3. Add variant_id (nullable) to product_images
-- 4. Create promotions + promotion_variants
-- 5. Add variant_name to order_items (variant_id becomes required)

-- ---------------------------------------------------------------------------
-- 1. products: drop base_price, discount_percent, stock_quantity
-- ---------------------------------------------------------------------------
ALTER TABLE products DROP INDEX idx_products_price;
ALTER TABLE products DROP COLUMN base_price;
ALTER TABLE products DROP COLUMN discount_percent;
ALTER TABLE products DROP COLUMN stock_quantity;

-- ---------------------------------------------------------------------------
-- 2. product_variants: add is_active
-- ---------------------------------------------------------------------------
ALTER TABLE product_variants
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE AFTER image_url;

CREATE INDEX idx_variants_product_active ON product_variants (product_id, is_active);

-- ---------------------------------------------------------------------------
-- 3. product_images: add variant_id (nullable)
--    variant_id NULL  -> shared product image
--    variant_id set   -> variant-specific image
-- ---------------------------------------------------------------------------
ALTER TABLE product_images
    ADD COLUMN variant_id BIGINT NULL AFTER product_id;

ALTER TABLE product_images
    ADD CONSTRAINT fk_images_variant FOREIGN KEY (variant_id)
        REFERENCES product_variants (id) ON DELETE CASCADE;

CREATE INDEX idx_images_variant ON product_images (variant_id);

-- ---------------------------------------------------------------------------
-- 4. promotions + promotion_variants
-- ---------------------------------------------------------------------------
CREATE TABLE promotions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    description         VARCHAR(500),
    discount_type       VARCHAR(20)  NOT NULL,
    discount_value      DECIMAL(12,2) NOT NULL,
    max_discount_amount DECIMAL(12,2),
    start_date          DATETIME     NOT NULL,
    end_date            DATETIME     NOT NULL,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_promotions_active (is_active),
    INDEX idx_promotions_dates (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE promotion_variants (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    variant_id   BIGINT NOT NULL,
    CONSTRAINT fk_pv_promotion FOREIGN KEY (promotion_id) REFERENCES promotions (id) ON DELETE CASCADE,
    CONSTRAINT fk_pv_variant   FOREIGN KEY (variant_id)   REFERENCES product_variants (id) ON DELETE CASCADE,
    UNIQUE KEY uk_promotion_variant (promotion_id, variant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5. order_items: add variant_name; variant_id becomes required
-- ---------------------------------------------------------------------------
ALTER TABLE order_items
    ADD COLUMN variant_name VARCHAR(255) NULL AFTER variant_sku;

-- Every product must have at least one variant. Backfill a default variant for
-- any product that currently has none (price/stock default to 0; admin will set
-- real values).
INSERT INTO product_variants (product_id, sku, color, size, price, stock_quantity, image_url, is_active)
SELECT p.id, CONCAT('DEFAULT-', UPPER(REPLACE(p.slug, '-', '-')), '-', p.id), NULL, NULL, 0, 0, NULL, TRUE
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM product_variants v WHERE v.product_id = p.id);
