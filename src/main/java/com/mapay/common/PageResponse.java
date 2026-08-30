package com.mapay.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "分页列表响应，兼容 Layui 表格格式")
public record PageResponse<T>(
        @Schema(description = "业务状态码，0 表示成功", example = "0")
        int code,
        @Schema(description = "响应提示信息", example = "OK")
        String msg,
        @Schema(description = "总记录数", example = "100")
        long count,
        @Schema(description = "当前页数据列表")
        List<T> data) {
    public static <T> PageResponse<T> of(long count, List<T> data) {
        return new PageResponse<>(0, "OK", count, data);
    }
}
