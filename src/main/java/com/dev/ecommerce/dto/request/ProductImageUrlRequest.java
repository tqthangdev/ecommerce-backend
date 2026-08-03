package com.dev.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductImageUrlRequest {

    @NotBlank
    @Size(max = 2048)
    @Pattern(regexp = "^https?://.+", message = "Image URL must be a valid http(s) URL")
    private String imageUrl;
}