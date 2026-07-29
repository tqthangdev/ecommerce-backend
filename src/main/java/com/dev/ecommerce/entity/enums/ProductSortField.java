package com.dev.ecommerce.entity.enums;

public enum ProductSortField {
    CREATED_AT("createdAt"),
    PRICE("basePrice"),
    NAME("name");

    private final String property;

    ProductSortField(String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }
}
