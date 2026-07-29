package com.dev.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank
    @Size(min = 2, max = 150)
    private String name;

    @Size(max = 500)
    private String imageUrl;

    private String description;

    private Boolean active;
}