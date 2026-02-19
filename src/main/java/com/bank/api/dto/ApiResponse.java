package com.bank.api.dto;

public record ApiResponse<T>(Number value, String text, T data) {

    public static <T> ApiResponse<T> ok(Number value, T data) {
        return new ApiResponse<>(value, "", data);
    }

    public static <T> ApiResponse<T> fail(Number value, String text) {
        return new ApiResponse<>(value, text, null);
    }
}
