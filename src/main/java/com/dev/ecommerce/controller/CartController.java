package com.dev.ecommerce.controller;

import com.dev.ecommerce.common.ApiResponse;
import com.dev.ecommerce.dto.request.AddToCartRequest;
import com.dev.ecommerce.dto.request.UpdateCartItemRequest;
import com.dev.ecommerce.dto.response.CartResponse;
import com.dev.ecommerce.security.UserPrincipal;
import com.dev.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart operations")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current cart")
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success("Cart fetched", cartService.getCart(principal.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add item to cart")
    public ApiResponse<CartResponse> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToCartRequest request
    ) {
        return ApiResponse.success("Item added to cart", cartService.addItem(principal.getId(), request));
    }

    @PutMapping("/items")
    @Operation(summary = "Update cart item quantity")
    public ApiResponse<CartResponse> updateItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long variantId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return ApiResponse.success("Cart updated",
                cartService.updateItem(principal.getId(), variantId, request));
    }

    @DeleteMapping("/items")
    @Operation(summary = "Remove item from cart")
    public ApiResponse<CartResponse> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long variantId
    ) {
        return ApiResponse.success("Item removed",
                cartService.removeItem(principal.getId(), variantId));
    }

    @DeleteMapping
    @Operation(summary = "Clear cart")
    public ApiResponse<Void> clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        cartService.clearCart(principal.getId());
        return ApiResponse.success("Cart cleared", null);
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge guest cart into user cart after login")
    public ApiResponse<CartResponse> mergeCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> guestCart
    ) {
        return ApiResponse.success("Cart merged",
                cartService.mergeCart(principal.getId(), guestCart));
    }
}
