package com.dev.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(resource + " not found with id: " + identifier, HttpStatus.NOT_FOUND);
    }
}
