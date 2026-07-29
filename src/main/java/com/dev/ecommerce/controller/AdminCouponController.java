package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.request.CouponRequest;
import com.dev.ecommerce.dto.response.CouponResponse;
import com.dev.ecommerce.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Coupons", description = "Admin coupon management")
public class AdminCouponController {

    private final CouponService couponService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a coupon")
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return ApiResponse.success("Coupon created", couponService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a coupon")
    public ApiResponse<CouponResponse> update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ApiResponse.success("Coupon updated", couponService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a coupon")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ApiResponse.success("Coupon deleted", null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get coupon by id")
    public ApiResponse<CouponResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Coupon fetched", couponService.getById(id));
    }
}
