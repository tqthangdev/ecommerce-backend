package com.dev.ecommerce.service;

import com.dev.ecommerce.dto.response.CartItemResponse;
import com.dev.ecommerce.dto.response.CartResponse;
import com.dev.ecommerce.entity.Product;
import com.dev.ecommerce.entity.ProductVariant;
import com.dev.ecommerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final ProductVariantRepository variantRepository;
    private final PromotionService promotionService;

    public void addItem(Long userId, Long variantId, int quantity) {
        String cartKey = cartKey(userId);
        String itemField = itemField(variantId);

        // Keep the original addedAt if the item already exists, so re-adding or
        // changing quantity does not reorder the cart.
        long addedAt = System.currentTimeMillis();
        Object existing = redisTemplate.opsForHash().get(cartKey, itemField);
        if (existing instanceof Map) {
            Object existingAddedAt = ((Map<?, ?>) existing).get("addedAt");
            if (existingAddedAt != null) {
                addedAt = toLong(existingAddedAt);
            }
        }

        Map<Object, Object> item = new HashMap<>();
        item.put("variantId", variantId);
        item.put("quantity", quantity);
        item.put("addedAt", addedAt);

        redisTemplate.opsForHash().put(cartKey, itemField, item);
        redisTemplate.expire(cartKey, CART_TTL_DAYS, TimeUnit.DAYS);
    }

    public void updateItemQuantity(Long userId, Long variantId, int quantity) {
        String cartKey = cartKey(userId);
        String itemField = itemField(variantId);

        if (Boolean.FALSE.equals(redisTemplate.opsForHash().hasKey(cartKey, itemField))) {
            return;
        }

        if (quantity <= 0) {
            removeItem(userId, variantId);
            return;
        }

        // Preserve the original addedAt so quantity updates don't reorder the cart.
        long addedAt = System.currentTimeMillis();
        Object existing = redisTemplate.opsForHash().get(cartKey, itemField);
        if (existing instanceof Map) {
            Object existingAddedAt = ((Map<?, ?>) existing).get("addedAt");
            if (existingAddedAt != null) {
                addedAt = toLong(existingAddedAt);
            }
        }

        Map<Object, Object> item = new HashMap<>();
        item.put("variantId", variantId);
        item.put("quantity", quantity);
        item.put("addedAt", addedAt);

        redisTemplate.opsForHash().put(cartKey, itemField, item);
    }

    public void removeItem(Long userId, Long variantId) {
        String cartKey = cartKey(userId);
        String itemField = itemField(variantId);
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
                .sorted(Comparator.comparingLong(CartItemResponse::getAddedAt))
                .collect(Collectors.toList());

        return buildCartResponse(items);
    }

    public CartResponse mergeGuestCart(Long userId, Map<String, Object> guestCartData) {
        if (guestCartData == null || guestCartData.isEmpty()) {
            return getCart(userId);
        }

        // guest cart format: { "item:<variantId>" -> { variantId, quantity } }
        for (Object value : guestCartData.values()) {
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) value;
                Long variantId = toLong(item.get("variantId"));
                Integer quantity = toInt(item.get("quantity"));
                if (variantId != null && quantity != null && quantity > 0) {
                    addItem(userId, variantId, quantity);
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

            Long variantId = toLong(data.get("variantId"));
            Integer quantity = toInt(data.get("quantity"));
            Long addedAt = toLong(data.get("addedAt"));

            if (variantId == null || quantity == null) return null;

            Optional<ProductVariant> variantOpt = variantRepository.findByIdWithProduct(variantId);
            if (variantOpt.isEmpty()) return null;

            ProductVariant variant = variantOpt.get();
            if (!variant.isActive() || !variant.getProduct().isActive()) return null;

            Product product = variant.getProduct();
            BigDecimal unitPrice = variant.getPrice();
            BigDecimal effectivePrice = promotionService.resolveEffectivePrice(variant);
            String imageUrl = StringUtils.hasText(variant.getImageUrl())
                    ? variant.getImageUrl()
                    : product.getImagesSorted().stream()
                            .findFirst()
                            .map(img -> img.getImageUrl())
                            .orElse(null);

            BigDecimal subtotal = effectivePrice.multiply(BigDecimal.valueOf(quantity));

            return CartItemResponse.builder()
                    .productId(product.getId())
                    .variantId(variantId)
                    .productName(product.getName())
                    .productSlug(product.getSlug())
                    .variantSku(variant.getSku())
                    .variantName(buildVariantName(variant))
                    .color(variant.getColor())
                    .size(variant.getSize())
                    .imageUrl(imageUrl)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .effectivePrice(effectivePrice)
                    .subtotal(subtotal)
                    .stockAvailable(variant.getStockQuantity())
                    .addedAt(addedAt != null ? addedAt : 0L)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse cart item: {}", e.getMessage());
            return null;
        }
    }

    private String buildVariantName(ProductVariant variant) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(variant.getColor())) parts.add(variant.getColor());
        if (StringUtils.hasText(variant.getSize())) parts.add(variant.getSize());
        return parts.isEmpty() ? variant.getSku() : String.join(" / ", parts);
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

    private String itemField(Long variantId) {
        return CART_ITEM_FIELD_PREFIX + variantId;
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
