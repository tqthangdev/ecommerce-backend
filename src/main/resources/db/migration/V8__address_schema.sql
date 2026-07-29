-- Sprint 4: Address schema

CREATE TABLE addresses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    recipient_name  VARCHAR(100) NOT NULL,
    phone           VARCHAR(20) NOT NULL,
    province_code   VARCHAR(20) NOT NULL,
    province_name   VARCHAR(100) NOT NULL,
    district_code   VARCHAR(20) NOT NULL,
    district_name   VARCHAR(100) NOT NULL,
    ward_code       VARCHAR(20) NOT NULL,
    ward_name       VARCHAR(100) NOT NULL,
    street_address  VARCHAR(500) NOT NULL,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    label           VARCHAR(50),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_addresses_user (user_id),
    INDEX idx_addresses_default (user_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
