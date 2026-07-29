-- Sprint 3: Product, ProductVariant, ProductImage schema

CREATE TABLE products (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    slug              VARCHAR(280) NOT NULL UNIQUE,
    description       TEXT,
    base_price        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_percent  DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    stock_quantity    INT           NOT NULL DEFAULT 0,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    is_featured       BOOLEAN       NOT NULL DEFAULT FALSE,
    view_count        BIGINT        NOT NULL DEFAULT 0,
    category_id       BIGINT        NOT NULL,
    brand_id          BIGINT        NOT NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_products_brand    FOREIGN KEY (brand_id)    REFERENCES brands (id)    ON DELETE RESTRICT,
    INDEX idx_products_slug (slug),
    INDEX idx_products_category (category_id),
    INDEX idx_products_brand (brand_id),
    INDEX idx_products_active (is_active),
    INDEX idx_products_price (base_price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_variants (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id     BIGINT         NOT NULL,
    sku            VARCHAR(80)    NOT NULL UNIQUE,
    color          VARCHAR(50),
    size           VARCHAR(50),
    price          DECIMAL(12,2)  NOT NULL,
    stock_quantity INT            NOT NULL DEFAULT 0,
    image_url      VARCHAR(500),
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    INDEX idx_variants_product (product_id),
    INDEX idx_variants_sku (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_images (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT       NOT NULL,
    image_url     VARCHAR(500) NOT NULL,
    alt_text      VARCHAR(255),
    display_order INT          NOT NULL DEFAULT 0,
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    INDEX idx_images_product (product_id),
    INDEX idx_images_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
