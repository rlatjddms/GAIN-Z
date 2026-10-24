package com.project.gainz.global.common.dto;

import com.project.gainz.global.error.ErrorCode;

public record ApiResponse<T>(
        boolean success,
        String message,
        String code,
        T data
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "요청이 성공적으로 처리되었습니다.", null, data);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode, null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, T data) {
        return new ApiResponse<>(false, errorCode.getMessage(), errorCode.getCode(), data);
    }

    public static <T> ApiResponse<T> fail(int status, String code, String message) {
        return new ApiResponse<>(false, message, code, null);
    }
}
