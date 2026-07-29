package com.dev.ecommerce.service;

import com.dev.ecommerce.dto.response.CartItemResponse;
import com.dev.ecommerce.dto.response.CartResponse;
import com.dev.ecommerce.entity.Product;
import com.dev.ecommerce.entity.ProductVariant;
import com.dev.ecommerce.mapper.ProductMapper;
import com.dev.ecommerce.repository.ProductRepository;
import com.dev.ecommerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartRedisService {

    private static final String CART_KEY_PREFIX = "cart:";
    private static final String CART_ITEM_FIELD_PREFIX = "item:";
    private static final long CART_TTL_DAYS = 30;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    public void addItem(Long userId, Long productId, Long variantId, int quantity) {
        String cartKey = cartKey(userId);
        String itemField = itemField(productId, variantId);

        Map<Object, Object> item = new HashMap<>();
        item.put("productId", productId);
        item.put("variantId", variantId);
        item.put("quantity", quantity);
        item.put("addedAt", System.currentTimeMillis());

        redisTemplate.opsForHash().put(cartKey, itemField, item);
        redisTemplate.expire(cartKey, CART_TTL_DAYS, TimeUnit.DAYS);
    }

    public void updateItemQuantity(Long userId, Long productId, Long variantId, int quantity) {
        String cartKey = cartKey(userId);
        String itemField = itemField(productId, variantId);

        if (Boolean.FALSE.equals(redisTemplate.opsForHash().hasKey(cartKey, itemField))) {
            return;
        }

        if (quantity <= 0) {
            removeItem(userId, productId, variantId);
            return;
        }

        Map<Object, Object> item = new HashMap<>();
        item.put("productId", productId);
        item.put("variantId", variantId);
        item.put("quantity", quantity);
        item.put("addedAt", System.currentTimeMillis());

        redisTemplate.opsForHash().put(cartKey, itemField, item);
    }

    public void removeItem(Long userId, Long productId, Long variantId) {
        String cartKey = cartKey(userId);
        String itemField = itemField(productId, variantId);
        redisTemplate.opsForHash().delete(cartKey, itemField);
    }

    public void clearCart(Long userId) {
        String cartKey = cartKey(userId);
        redisTemplate.delete(cartKey);
    }

    public CartResponse getCart(Long userId) {
        String cartKey = cartKey(userId);
        Map<Object, Object> rawItems = redisTemplate.opsForHash().entries(cartKey);

        if (rawItems.isEmpty()) {
            return emptyCart();
        }

        List<CartItemResponse> items = rawItems.values().stream()
                .map(this::parseCartItem)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return buildCartResponse(items);
    }

    public CartResponse mergeGuestCart(Long userId, Map<String, Object> guestCartData) {
        if (guestCartData == null || guestCartData.isEmpty()) {
            return getCart(userId);
        }

        // guest cart format: { "item:productId:variantId" -> { productId, variantId, quantity } }
        for (Object value : guestCartData.values()) {
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) value;
                Long productId = toLong(item.get("productId"));
                Long variantId = toLong(item.get("variantId"));
                Integer quantity = toInt(item.get("quantity"));
                if (productId != null && quantity != null && quantity > 0) {
                    addItem(userId, productId, variantId, quantity);
                }
            }
        }
        return getCart(userId);
    }

    @SuppressWarnings("unchecked")
    private CartItemResponse parseCartItem(Object raw) {
        try {
            if (!(raw instanceof Map)) return null;
            Map<String, Object> data = (Map<String, Object>) raw;

            Long productId = toLong(data.get("productId"));
            Long variantId = toLong(data.get("variantId"));
            Integer quantity = toInt(data.get("quantity"));

            if (productId == null || quantity == null) return null;

            Optional<Product> productOpt = productRepository.findWithDetailsById(productId);
            if (productOpt.isEmpty()) return null;

            Product product = productOpt.get();
            BigDecimal unitPrice = product.getBasePrice();
            BigDecimal effectivePrice = product.getEffectivePrice();
            String imageUrl = product.getImagesSorted().stream()
                    .findFirst()
                    .map(img -> img.getImageUrl())
                    .orElse(null);
            String color = null;
            String size = null;
            String variantSku = null;
            int stockAvailable = product.getStockQuantity();

            if (variantId != null) {
                Optional<ProductVariant> variantOpt = variantRepository.findById(variantId);
                if (variantOpt.isPresent()) {
                    ProductVariant variant = variantOpt.get();
                    unitPrice = variant.getPrice();
                    effectivePrice = variant.getPrice();
                    color = variant.getColor();
                    size = variant.getSize();
                    variantSku = variant.getSku();
                    stockAvailable = variant.getStockQuantity();
                    if (variant.getImageUrl() != null) {
                        imageUrl = variant.getImageUrl();
                    }
                }
            }

            BigDecimal subtotal = effectivePrice.multiply(BigDecimal.valueOf(quantity));

            return CartItemResponse.builder()
                    .productId(productId)
                    .variantId(variantId)
                    .productName(product.getName())
                    .productSlug(product.getSlug())
                    .variantSku(variantSku)
                    .color(color)
                    .size(size)
                    .imageUrl(imageUrl)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .effectivePrice(effectivePrice)
                    .subtotal(subtotal)
                    .stockAvailable(stockAvailable)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse cart item: {}", e.getMessage());
            return null;
        }
    }

    private CartResponse buildCartResponse(List<CartItemResponse> items) {
        int totalQuantity = items.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .items(items)
                .totalItems(items.size())
                .totalQuantity(totalQuantity)
                .subtotal(subtotal)
                .discount(BigDecimal.ZERO)
                .total(subtotal)
                .build();
    }

    private CartResponse emptyCart() {
        return CartResponse.builder()
                .items(Collections.emptyList())
                .totalItems(0)
                .totalQuantity(0)
                .subtotal(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .build();
    }

    private String cartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private String itemField(Long productId, Long variantId) {
        return CART_ITEM_FIELD_PREFIX + productId + ":" + (variantId != null ? variantId : "0");
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Long) return ((Long) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }
}
