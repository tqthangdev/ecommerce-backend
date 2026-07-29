package com.dev.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "endpoint", nullable = false, length = 255)
    private String endpoint;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_status")
    private int responseStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public IdempotencyRecord(String idempotencyKey, Long userId, String endpoint,
                             String responseBody, int responseStatus,
                             LocalDateTime expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.endpoint = endpoint;
        this.responseBody = responseBody;
        this.responseStatus = responseStatus;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }
}
