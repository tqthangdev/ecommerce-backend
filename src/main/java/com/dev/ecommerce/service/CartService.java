package com.dev.ecommerce.service;

import com.dev.ecommerce.dto.request.AddToCartRequest;
import com.dev.ecommerce.dto.request.UpdateCartItemRequest;
import com.dev.ecommerce.dto.response.CartResponse;
import com.dev.ecommerce.entity.Product;
import com.dev.ecommerce.entity.ProductVariant;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.repository.ProductRepository;
import com.dev.ecommerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRedisService cartRedisService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    public CartResponse getCart(Long userId) {
        return cartRedisService.getCart(userId);
    }

    public CartResponse addItem(Long userId, AddToCartRequest request) {
        validateProduct(request.getProductId());

        if (request.getVariantId() != null) {
            validateVariant(request.getVariantId(), request.getProductId());
        }

        cartRedisService.addItem(userId, request.getProductId(), request.getVariantId(), request.getQuantity());
        return cartRedisService.getCart(userId);
    }

    public CartResponse updateItem(Long userId, Long productId, Long variantId, UpdateCartItemRequest request) {
        cartRedisService.updateItemQuantity(userId, productId, variantId, request.getQuantity());
        return cartRedisService.getCart(userId);
    }

    public CartResponse removeItem(Long userId, Long productId, Long variantId) {
        cartRedisService.removeItem(userId, productId, variantId);
        return cartRedisService.getCart(userId);
    }

    public void clearCart(Long userId) {
        cartRedisService.clearCart(userId);
    }

    public CartResponse mergeCart(Long userId, java.util.Map<String, Object> guestCartData) {
        return cartRedisService.mergeGuestCart(userId, guestCartData);
    }

    private void validateProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (!product.isActive()) {
            throw new BusinessException("Product is not available", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateVariant(Long variantId, Long productId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException("Variant does not belong to this product", HttpStatus.BAD_REQUEST);
        }
    }
}
