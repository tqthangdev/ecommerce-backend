package com.dev.ecommerce.service;

import com.dev.ecommerce.entity.ProductVariant;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductVariantRepository variantRepository;
    // Cross-bean call (not self-invocation), so ProductService's @CacheEvict proxy
    // fires normally. Only evicts the single CACHE_PRODUCT_DETAIL entry for this
    // product id — cheap, precise, safe to call on every order.
    private final ProductService productService;

    /**
     * Simple DTO so callers (e.g. checkout) can pass a whole set of lines
     * to be deducted atomically, in a deterministic lock order.
     */
    public record StockLine(Long variantId, int quantity) {}

    /**
     * Deducts stock from a variant using pessimistic locking (SELECT FOR UPDATE).
     * Throws exception if insufficient stock.
     */
    @Transactional
    public void deductStock(Long variantId, int quantity) {
        if (quantity <= 0) {
            log.warn("deductStock called with non-positive quantity={} variantId={}",
                    quantity, variantId);
            return;
        }

        variantRepository.findByIdWithLock(variantId).ifPresentOrElse(
                variant -> deductVariantStock(variant, quantity),
                () -> { throw new ResourceNotFoundException("ProductVariant", variantId); }
        );
    }

    /**
     * Deducts stock for multiple lines (e.g. a whole order) inside ONE transaction.
     *
     * IMPORTANT: lines are sorted into a deterministic order (by variantId) before
     * locking. This prevents deadlocks that can otherwise happen when two concurrent
     * checkouts touch the same set of variants but in a different order (e.g. because
     * their carts were built in a different sequence).
     *
     * Callers (OrderService.checkout) should use this instead of looping and calling
     * deductStock(...) once per item.
     */
    @Transactional
    public void deductStockBatch(List<StockLine> lines) {
        lines.stream()
                .sorted(Comparator.comparing((StockLine l) -> "V" + l.variantId()))
                .forEach(line -> deductStock(line.variantId(), line.quantity()));
    }

    /**
     * Restores stock on a variant (called when order cancelled/timeout).
     */
    @Transactional
    public void restoreStock(Long variantId, int quantity) {
        if (quantity <= 0) {
            log.warn("restoreStock called with non-positive quantity={} variantId={}",
                    quantity, variantId);
            return;
        }

        variantRepository.findByIdWithLock(variantId).ifPresent(
                variant -> {
                    variant.setStockQuantity(variant.getStockQuantity() + quantity);
                    variantRepository.save(variant);
                    productService.evictProductDetailCache(variant.getProduct().getId());
                }
        );
    }

    /**
     * Best-effort, non-authoritative pre-check used for fast user feedback (e.g. showing
     * "out of stock" in the UI before the user even attempts checkout). This does NOT lock
     * rows and therefore does NOT guarantee stock will still be available by the time
     * deductStock/deductStockBatch actually runs — that check (with a lock) is the only one
     * that is authoritative. Do not rely on this alone to prevent overselling.
     */
    @Transactional(readOnly = true)
    public void validateStock(Long variantId, int requestedQty) {
        if (requestedQty <= 0) return;

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
        if (variant.getStockQuantity() < requestedQty) {
            log.warn(
                    "Insufficient stock. Product={}, SKU={}, variantId={}, requested={}, available={}",
                    variant.getProduct().getName(),
                    variant.getSku(),
                    variant.getId(),
                    requestedQty,
                    variant.getStockQuantity()
            );

            throw new BusinessException(
                    "Not enough stock for " + variant.getProduct().getName()
                            + ". Available: " + variant.getStockQuantity(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void deductVariantStock(ProductVariant variant, int quantity) {
        if (variant.getStockQuantity() < quantity) {
            log.warn(
                    "Insufficient stock. Product={}, SKU={}, variantId={}, requested={}, available={}",
                    variant.getProduct().getName(),
                    variant.getSku(),
                    variant.getId(),
                    quantity,
                    variant.getStockQuantity()
            );
            throw new BusinessException(
                    "Not enough stock for "
                            + variant.getProduct().getName()
                            + ". Available: "
                            + variant.getStockQuantity(),
                    HttpStatus.BAD_REQUEST
            );
        }
        variant.setStockQuantity(variant.getStockQuantity() - quantity);
        variantRepository.save(variant);
        productService.evictProductDetailCache(variant.getProduct().getId());
    }
}
