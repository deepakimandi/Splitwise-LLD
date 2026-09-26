package com.lld.splitwise.controller;

/** Stand-in for an HTTP response: status code + body or error message. */
public record ApiResponse<T>(int status, T body, String error) {
    public static <T> ApiResponse<T> ok(T body) {
        return new ApiResponse<>(200, body, null);
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, null, message);
    }

    public boolean isOk() {
        return status >= 200 && status < 300;
    }
}
