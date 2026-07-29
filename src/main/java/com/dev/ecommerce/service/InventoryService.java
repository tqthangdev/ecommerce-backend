package com.dev.ecommerce.service;

import com.dev.ecommerce.entity.ProductVariant;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.repository.ProductRepository;
import com.dev.ecommerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    /**
     * Deducts stock using pessimistic locking (SELECT FOR UPDATE).
     * Throws exception if insufficient stock.
     */
    @Transactional
    public void deductStock(Long productId, Long variantId, int quantity) {
        if (quantity <= 0) return;

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
     * Restores stock (called when order cancelled/timeout).
     */
    @Transactional
    public void restoreStock(Long productId, Long variantId, int quantity) {
        if (quantity <= 0) return;

        if (variantId != null) {
            variantRepository.findByIdWithLock(variantId).ifPresent(
                    variant -> {
                        variant.setStockQuantity(variant.getStockQuantity() + quantity);
                        variantRepository.save(variant);
                    }
            );
        } else {
            productRepository.findByIdWithLock(productId).ifPresent(
                    product -> {
                        product.setStockQuantity(product.getStockQuantity() + quantity);
                        productRepository.save(product);
                    }
            );
        }
    }

    /**
     * Validates all items in cart have sufficient stock.
     */
    @Transactional(readOnly = true)
    public void validateStock(Long productId, Long variantId, int requestedQty) {
        if (requestedQty <= 0) return;

        if (variantId != null) {
            ProductVariant variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
            if (variant.getStockQuantity() < requestedQty) {
                throw new BusinessException(
                        "Insufficient stock for variant SKU: " + variant.getSku() +
                                ". Available: " + variant.getStockQuantity(),
                        HttpStatus.BAD_REQUEST
                );
            }
        } else {
            var product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
            if (product.getStockQuantity() < requestedQty) {
                throw new BusinessException(
                        "Insufficient stock for product: " + product.getName() +
                                ". Available: " + product.getStockQuantity(),
                        HttpStatus.BAD_REQUEST
                );
            }
        }
    }

    private void deductProductStock(com.dev.ecommerce.entity.Product product, int quantity) {
        if (product.getStockQuantity() < quantity) {
            throw new BusinessException(
                    "Insufficient stock for product: " + product.getName() +
                            ". Requested: " + quantity + ", Available: " + product.getStockQuantity(),
                    HttpStatus.BAD_REQUEST
            );
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
    }

    private void deductVariantStock(ProductVariant variant, int quantity) {
        if (variant.getStockQuantity() < quantity) {
            throw new BusinessException(
                    "Insufficient stock for variant SKU: " + variant.getSku() +
                            ". Requested: " + quantity + ", Available: " + variant.getStockQuantity(),
                    HttpStatus.BAD_REQUEST
            );
        }
        variant.setStockQuantity(variant.getStockQuantity() - quantity);
        variantRepository.save(variant);
    }
}
