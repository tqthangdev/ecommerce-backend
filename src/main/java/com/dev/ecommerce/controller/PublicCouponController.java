package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.request.ApplyCouponRequest;
import com.dev.ecommerce.dto.response.CouponResponse;
import com.dev.ecommerce.dto.response.CouponValidationResponse;
import com.dev.ecommerce.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Public coupon listing and validation")
public class PublicCouponController {

    private final CouponService couponService;

    @GetMapping
    @Operation(summary = "List all active coupons")
    public ApiResponse<List<CouponResponse>> listActive() {
        return ApiResponse.success("Coupons fetched", couponService.listActive());
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate and calculate coupon discount")
    public ApiResponse<CouponValidationResponse> validate(@Valid @RequestBody ApplyCouponRequest request) {
        return ApiResponse.success("Validation result", couponService.validateAndCalculate(request));
    }
}
