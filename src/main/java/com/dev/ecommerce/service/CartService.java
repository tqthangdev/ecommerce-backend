package com.dev.ecommerce.service;

import com.dev.ecommerce.dto.request.AddToCartRequest;
import com.dev.ecommerce.dto.request.UpdateCartItemRequest;
import com.dev.ecommerce.dto.response.CartResponse;
import com.dev.ecommerce.entity.ProductVariant;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRedisService cartRedisService;
    private final ProductVariantRepository variantRepository;

    public CartResponse getCart(Long userId) {
        return cartRedisService.getCart(userId);
    }

    @Transactional(readOnly = true)
    public CartResponse addItem(Long userId, AddToCartRequest request) {
        validateVariant(request.getVariantId());
        cartRedisService.addItem(userId, request.getVariantId(), request.getQuantity());
        return cartRedisService.getCart(userId);
    }

    public CartResponse updateItem(Long userId, Long variantId, UpdateCartItemRequest request) {
        cartRedisService.updateItemQuantity(userId, variantId, request.getQuantity());
        return cartRedisService.getCart(userId);
    }

    public CartResponse removeItem(Long userId, Long variantId) {
        cartRedisService.removeItem(userId, variantId);
        return cartRedisService.getCart(userId);
    }

    public void clearCart(Long userId) {
        cartRedisService.clearCart(userId);
    }

    public CartResponse mergeCart(Long userId, java.util.Map<String, Object> guestCartData) {
        return cartRedisService.mergeGuestCart(userId, guestCartData);
    }

    private void validateVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
        if (!variant.isActive()) {
            throw new BusinessException("Variant is not available", HttpStatus.BAD_REQUEST);
        }
        if (!variant.getProduct().isActive()) {
            throw new BusinessException("Product is not available", HttpStatus.BAD_REQUEST);
        }
    }
}
