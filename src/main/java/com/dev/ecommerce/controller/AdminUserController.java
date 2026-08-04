package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.request.AdminUserCreateRequest;
import com.dev.ecommerce.dto.request.AdminUserUpdateRequest;
import com.dev.ecommerce.dto.response.AdminUserResponse;
import com.dev.ecommerce.dto.response.RoleResponse;
import com.dev.ecommerce.service.AdminUserService;
import com.dev.ecommerce.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
@Tag(name = "Admin - Users", description = "Admin user management operations")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final RoleService roleService;

    @GetMapping("/roles")
    @Operation(summary = "Get available roles")
    public ApiResponse<List<RoleResponse>> getRoles() {
        return ApiResponse.success(
                "Get roles successfully",
                roleService.getAvailableRoles()
        );
    }

    @PostMapping
    @Operation(summary = "Create user")
    public ApiResponse<AdminUserResponse> create(
            @Valid @RequestBody AdminUserCreateRequest request
    ) {
        return ApiResponse.success(
                "User created",
                adminUserService.create(request)
        );
    }

    @GetMapping
    @Operation(summary = "List users")
    public ApiResponse<Object> list(
            Authentication authentication,
            Pageable pageable
    ) {
        return ApiResponse.success(
                "Users fetched",
                adminUserService.list(authentication, pageable)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id")
    public ApiResponse<AdminUserResponse> get(
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                "User fetched",
                adminUserService.getById(id)
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public ApiResponse<AdminUserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return ApiResponse.success(
                "User updated",
                adminUserService.update(id, request)
        );
    }
}