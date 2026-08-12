package com.dev.ecommerce.repository.specification;

import com.dev.ecommerce.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null
                ? cb.conjunction()
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> hasBrand(Long brandId) {
        return (root, query, cb) -> brandId == null
                ? cb.conjunction()
                : cb.equal(root.get("brand").get("id"), brandId);
    }

    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) {
                return cb.conjunction();
            }

            jakarta.persistence.criteria.Subquery<Long> sub = query.subquery(Long.class);
            jakarta.persistence.criteria.Root<?> variant = sub.from(
                    com.dev.ecommerce.entity.ProductVariant.class
            );
            sub.select(variant.get("id"));

            Predicate isSameProduct = cb.equal(variant.get("product"), root);

            if (minPrice != null && maxPrice != null) {
                sub.where(
                        isSameProduct,
                        cb.between(variant.get("price"), minPrice, maxPrice)
                );
            } else if (minPrice != null) {
                sub.where(
                        isSameProduct,
                        cb.greaterThanOrEqualTo(variant.get("price"), minPrice)
                );
            } else {
                sub.where(
                        isSameProduct,
                        cb.lessThanOrEqualTo(variant.get("price"), maxPrice)
                );
            }

            return cb.exists(sub);
        };
    }

    public static Specification<Product> hasKeyword(String keyword) {
        return (root, query, cb) -> !StringUtils.hasText(keyword)
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Product> isActive(Boolean active) {
        return (root, query, cb) -> active == null
                ? cb.conjunction()
                : cb.equal(root.get("active"), active);
    }
}
