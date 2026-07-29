-- Sprint 1: Foundation schema bootstrap
-- Detailed tables (User, Product, Order...) will be added in later sprints.

CREATE TABLE IF NOT EXISTS app_metadata (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `key`       VARCHAR(100)  NOT NULL UNIQUE,
    `value`     VARCHAR(500)  NOT NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO app_metadata (`key`, `value`)
VALUES ('schema_version', '1.0.0')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
