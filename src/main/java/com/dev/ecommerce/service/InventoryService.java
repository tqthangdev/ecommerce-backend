package com.dev.ecommerce.service;

import com.dev.ecommerce.entity.ProductVariant;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.repository.ProductRepository;
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

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    // Cross-bean call (not self-invocation), so ProductService's @CacheEvict proxy
    // fires normally. Only evicts the single CACHE_PRODUCT_DETAIL entry for this
    // product id — cheap, precise, safe to call on every order.
    private final ProductService productService;

    /**
     * Simple DTO so callers (e.g. checkout) can pass a whole set of lines
     * to be deducted atomically, in a deterministic lock order.
     */
    public record StockLine(Long productId, Long variantId, int quantity) {}

    /**
     * Deducts stock using pessimistic locking (SELECT FOR UPDATE).
     * Throws exception if insufficient stock.
     */
    @Transactional
    public void deductStock(Long productId, Long variantId, int quantity) {
        if (quantity <= 0) {
            log.warn("deductStock called with non-positive quantity={} productId={} variantId={}",
                    quantity, productId, variantId);
            return;
        }

        if (variantId != null) {
            variantRepository.findByIdWithLock(variantId).ifPresentOrElse(
                    variant -> deductVariantStock(variant, quantity),
                    () -> { throw new ResourceNotFoundException("ProductVariant", variantId); }
            );
        } else {
            productRepository.findByIdWithLock(productId).ifPresentOrElse(
                    product -> deductProductStock(product, quantity),
                    () -> { throw new ResourceNotFoundException("Product", productId); }
            );
        }
    }

    /**
     * Deducts stock for multiple lines (e.g. a whole order) inside ONE transaction.
     *
     * IMPORTANT: lines are sorted into a deterministic order (by variantId if present,
     * else productId) before locking. This prevents deadlocks that can otherwise happen
     * when two concurrent checkouts touch the same set of products/variants but in a
     * different order (e.g. because their carts were built in a different sequence).
     *
     * Callers (OrderService.checkout) should use this instead of looping and calling
     * deductStock(...) once per item.
     */
    @Transactional
    public void deductStockBatch(List<StockLine> lines) {
        lines.stream()
                .sorted(Comparator.comparing(
                        (StockLine l) -> l.variantId() != null ? "V" + l.variantId() : "P" + l.productId()))
                .forEach(line -> deductStock(line.productId(), line.variantId(), line.quantity()));
    }

    /**
     * Restores stock (called when order cancelled/timeout).
     */
    @Transactional
    public void restoreStock(Long productId, Long variantId, int quantity) {
        if (quantity <= 0) {
            log.warn("restoreStock called with non-positive quantity={} productId={} variantId={}",
                    quantity, productId, variantId);
            return;
        }

        if (variantId != null) {
            variantRepository.findByIdWithLock(variantId).ifPresent(
                    variant -> {
                        variant.setStockQuantity(variant.getStockQuantity() + quantity);
                        variantRepository.save(variant);
                        // Keep Product.stockQuantity in sync when restoring variant stock too.
                        productRepository.adjustStockQuantity(variant.getProduct().getId(), quantity);
                        productService.evictProductDetailCache(variant.getProduct().getId());
                    }
            );
        } else {
            productRepository.findByIdWithLock(productId).ifPresent(
                    product -> {
                        product.setStockQuantity(product.getStockQuantity() + quantity);
                        productRepository.save(product);
                        productService.evictProductDetailCache(product.getId());
                    }
            );
        }
    }

    /**
     * Best-effort, non-authoritative pre-check used for fast user feedback (e.g. showing
     * "out of stock" in the UI before the user even attempts checkout). This does NOT lock
     * rows and therefore does NOT guarantee stock will still be available by the time
     * deductStock/deductStockBatch actually runs — that check (with a lock) is the only one
     * that is authoritative. Do not rely on this alone to prevent overselling.
     */
    @Transactional(readOnly = true)
    public void validateStock(Long productId, Long variantId, int requestedQty) {
        if (requestedQty <= 0) return;

        if (variantId != null) {
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
        } else {
            var product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
            if (product.getStockQuantity() < requestedQty) {
                log.warn(
                        "Insufficient stock. Product={}, productId={}, requested={}, available={}",
                        product.getName(),
                        product.getId(),
                        requestedQty,
                        product.getStockQuantity()
                );

                throw new BusinessException(
                        "Not enough stock for " + product.getName()
                                + ". Available: " + product.getStockQuantity(),
                        HttpStatus.BAD_REQUEST
                );
            }
        }
    }

    private void deductProductStock(com.dev.ecommerce.entity.Product product, int quantity) {
        if (product.getStockQuantity() < quantity) {
            log.warn(
                    "Insufficient stock. Product={}, productId={}, requested={}, available={}",
                    product.getName(),
                    product.getId(),
                    quantity,
                    product.getStockQuantity()
            );

            throw new BusinessException(
                    "Not enough stock for " + product.getName()
                            + ". Available: " + product.getStockQuantity(),
                    HttpStatus.BAD_REQUEST
            );
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
        productService.evictProductDetailCache(product.getId());
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

        // Keep the denormalized Product.stockQuantity (sum of all variants) in sync.
        // Uses an atomic UPDATE (no extra SELECT/lock, no full recalculateProduct scan)
        // since only the total changes here — variant prices/count are untouched, so
        // Product.basePrice does not need to be recomputed.
        productRepository.adjustStockQuantity(variant.getProduct().getId(), -quantity);
        productService.evictProductDetailCache(variant.getProduct().getId());
    }
}