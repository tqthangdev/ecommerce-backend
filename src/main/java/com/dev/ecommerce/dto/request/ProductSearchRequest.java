package com.dev.ecommerce.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchRequest {

    private Long categoryId;
    private Long brandId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String keyword;

    /** Format: "field,direction" e.g. "basePrice,asc" */
    private String sort;

    @PositiveOrZero
    private Integer page = 0;

    @PositiveOrZero
    private Integer size = 20;

    private Boolean active = Boolean.TRUE;

    @AssertTrue(message = "minPrice must be less than or equal to maxPrice")
    public boolean isPriceRangeValid() {
        if (minPrice == null || maxPrice == null) return true;
        return minPrice.compareTo(maxPrice) <= 0;
    }
}