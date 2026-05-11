package com.enftrms.prediction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final Pagination pagination;
    private final ApiError error;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }
    public static <T> ApiResponse<T> ok(T data, Pagination pagination) {
        return ApiResponse.<T>builder().success(true).data(data).pagination(pagination).build();
    }
    public static <T> ApiResponse<T> fail(String code, String message) {
        return ApiResponse.<T>builder().success(false).error(new ApiError(code, message)).build();
    }

    @Getter @AllArgsConstructor
    public static class ApiError {
        private final String code;
        private final String message;
    }
    @Getter @AllArgsConstructor @Builder
    public static class Pagination {
        private final int page;
        private final int size;
        private final long total;
    }
}
