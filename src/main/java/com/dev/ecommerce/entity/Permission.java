package com.dev.ecommerce.entity;

import com.dev.ecommerce.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(length = 255)
    private String description;

    public Permission(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
