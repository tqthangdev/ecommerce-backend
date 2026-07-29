-- Sprint 5: Idempotency records for preventing duplicate orders

CREATE TABLE idempotency_records (
    idempotency_key   VARCHAR(100) PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    endpoint          VARCHAR(255) NOT NULL,
    response_body     TEXT,
    response_status   INT,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at        DATETIME NOT NULL,
    INDEX idx_idempotency_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
