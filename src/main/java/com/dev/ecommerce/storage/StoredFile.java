package com.dev.ecommerce.storage;

public record StoredFile(String url, String key, long size, String contentType) {
}