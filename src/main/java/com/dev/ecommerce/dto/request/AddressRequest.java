package com.dev.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String recipientName;

    @NotBlank
    @Size(min = 10, max = 20)
    private String phone;

    @NotBlank
    private String provinceCode;

    @NotBlank
    private String provinceName;

    @NotBlank
    private String districtCode;

    @NotBlank
    private String districtName;

    @NotBlank
    private String wardCode;

    @NotBlank
    private String wardName;

    @NotBlank
    @Size(max = 500)
    private String streetAddress;

    private Boolean setAsDefault;

    @Size(max = 50)
    private String label;
}
