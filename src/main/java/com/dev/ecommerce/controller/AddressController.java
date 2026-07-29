package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.request.AddressRequest;
import com.dev.ecommerce.dto.response.AddressResponse;
import com.dev.ecommerce.security.UserPrincipal;
import com.dev.ecommerce.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "User shipping address management")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "List user addresses")
    public ApiResponse<List<AddressResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success("Addresses fetched", addressService.listByUser(principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address by id")
    public ApiResponse<AddressResponse> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.success("Address fetched", addressService.getById(principal.getId(), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create address")
    public ApiResponse<AddressResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddressRequest request
    ) {
        return ApiResponse.success("Address created",
                addressService.create(principal.getId(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update address")
    public ApiResponse<AddressResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request
    ) {
        return ApiResponse.success("Address updated",
                addressService.update(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        addressService.delete(principal.getId(), id);
        return ApiResponse.success("Address deleted", null);
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Set address as default")
    public ApiResponse<AddressResponse> setDefault(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.success("Default address updated",
                addressService.setDefault(principal.getId(), id));
    }
}
