package com.dev.ecommerce.common;

import com.dev.ecommerce.common.constants.AppConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.MDC;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String traceId;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                true,
                message,
                data,
                MDC.get(AppConstants.TRACE_ID_MDC_KEY),
                LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(
                false,
                message,
                null,
                MDC.get(AppConstants.TRACE_ID_MDC_KEY),
                LocalDateTime.now()
        );
    }
}
