CREATE TABLE user_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,

    action VARCHAR(50) NOT NULL,

    reason VARCHAR(255),

    performed_by BIGINT NOT NULL,

    created_at DATETIME NOT NULL,

    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_user_status_history_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_user_status_history_performed_by
        FOREIGN KEY (performed_by)
        REFERENCES users(id)
);

CREATE INDEX idx_user_status_history_user_id
    ON user_status_history(user_id);

CREATE INDEX idx_user_status_history_performed_by
    ON user_status_history(performed_by);