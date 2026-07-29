package com.dev.ecommerce.validation.validator;

import com.dev.ecommerce.validation.annotation.ValidPhone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private static final Pattern VIETNAM_PHONE_PATTERN =
            Pattern.compile("^(\\+84|0)(3|5|7|8|9)[0-9]{8}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return VIETNAM_PHONE_PATTERN.matcher(value).matches();
    }
}
