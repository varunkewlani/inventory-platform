package com.uphead.inventory.common.response;

/**
 * Uniform envelope for every API response, per the assignment spec's
 * {"success", "data", "meta"} / {"success", "error"} contract.
 */
public record ApiResponse<T>(boolean success, T data, Object meta, ApiError error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> success(T data, Object meta) {
        return new ApiResponse<>(true, data, meta, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, null, new ApiError(code, message));
    }
}
