package com.dev.ecommerce.repository;

import com.dev.ecommerce.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"category", "brand", "variants", "images"})
    Optional<Product> findWithDetailsById(Long id);

    @Query(value = "SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand LEFT JOIN FETCH p.variants LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findWithDetailsByIdFetch(Long id);

    @EntityGraph(attributePaths = {"category", "brand", "variants", "images"})
    Optional<Product> findWithDetailsBySlug(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    /**
     * Atomically adjusts the denormalized Product.stockQuantity by {@code delta}
     * (positive to add, negative to subtract). Plain UPDATE — no SELECT, no extra
     * row lock beyond what the UPDATE itself takes — so it's safe to call from
     * inside a transaction that's already holding a lock on the related
     * ProductVariant row (see InventoryService.deductVariantStock/restoreStock)
     * without adding deadlock risk or extra round trips.
     *
     * Does NOT touch basePrice — use ProductService.recalculateProduct() instead
     * when a variant's price, or the set of variants, actually changes.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :delta WHERE p.id = :id")
    int adjustStockQuantity(@Param("id") Long id, @Param("delta") int delta);
}