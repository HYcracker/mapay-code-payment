package com.mapay.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一接口响应")
public record ApiResponse<T>(
        @Schema(description = "业务状态码，0 表示成功，1 表示失败，401 表示未登录", example = "0")
        int code,
        @Schema(description = "响应提示信息", example = "操作成功")
        String msg,
        @Schema(description = "响应数据")
        T data) {
    public static <T> ApiResponse<T> ok(String msg, T data) {
        return new ApiResponse<>(0, msg, data);
    }

    public static <T> ApiResponse<T> fail(String msg) {
        return new ApiResponse<>(1, msg, null);
    }
}
