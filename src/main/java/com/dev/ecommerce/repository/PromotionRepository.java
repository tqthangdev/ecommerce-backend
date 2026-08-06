package com.dev.ecommerce.repository;

import com.dev.ecommerce.entity.Promotion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @EntityGraph(attributePaths = { "variants" })
    @Query("SELECT p FROM Promotion p WHERE p.id = :id")
    Optional<Promotion> findByIdWithVariants(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Promotion p JOIN p.variants v " +
            "WHERE v.id = :variantId AND p.active = true " +
            "AND p.startDate <= :now AND p.endDate >= :now " +
            "ORDER BY p.startDate ASC")
    List<Promotion> findActiveByVariantId(@Param("variantId") Long variantId, @Param("now") LocalDateTime now);
}
