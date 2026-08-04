package com.dev.ecommerce.dto.response;

import com.dev.ecommerce.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoleResponse {

    private String name;

    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getName().name());
    }
}