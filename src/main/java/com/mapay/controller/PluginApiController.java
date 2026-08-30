package com.mapay.controller;

import com.mapay.common.ApiResponse;
import com.mapay.common.PageResponse;
import com.mapay.service.PluginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/plugin", "/api/Plugin"})
@Tag(name = "插件中心", description = "插件列表、插件安装、更新、卸载和启停")
public class PluginApiController {
    private final PluginService plugins;

    public PluginApiController(PluginService plugins) {
        this.plugins = plugins;
    }

    @GetMapping({"/list", "/getPluginList"})
    @Operation(summary = "查询插件列表", description = "返回插件中心列表，兼容原后台 getPluginList 接口。")
    public PageResponse<Map<String, Object>> list(@Parameter(description = "显示状态筛选，沿用原后台参数") @RequestParam(defaultValue = "1") int show) {
        List<Map<String, Object>> data = plugins.list(show);
        return PageResponse.of(data.size(), data);
    }

    @GetMapping("/pluginOption")
    @Operation(summary = "查询插件选项", description = "返回可用于筛选或配置的插件选项。")
    public List<Map<String, Object>> pluginOption() {
        return plugins.options();
    }

    @PostMapping("/installPlugin")
    @Operation(summary = "安装插件", description = "请求体字段：platform 插件平台标识。")
    public Map<String, Object> install(@RequestBody Map<String, String> body) {
        plugins.install(body.get("platform"));
        return Map.of("code", 0, "msg", "安装成功", "state", 1);
    }

    @PostMapping("/updatePlugin")
    @Operation(summary = "更新插件", description = "请求体字段：platform 插件平台标识。")
    public ApiResponse<Void> update(@RequestBody Map<String, String> body) {
        plugins.install(body.get("platform"));
        return ApiResponse.ok("更新成功", null);
    }

    @PostMapping("/uninstallPlugin")
    @Operation(summary = "卸载插件", description = "请求体字段：platform 插件平台标识。")
    public ApiResponse<Void> uninstall(@RequestBody Map<String, String> body) {
        plugins.uninstall(body.get("platform"));
        return ApiResponse.ok("卸载成功", null);
    }

    @PostMapping("/pluginEnable")
    @Operation(summary = "启用或禁用插件", description = "请求体字段：platform 插件平台标识；state 状态，1 启用，0 禁用。")
    public ApiResponse<Void> enable(@RequestBody Map<String, String> body) {
        boolean ok = plugins.enable(body.get("platform"), Integer.parseInt(body.getOrDefault("state", "1")));
        return ok ? ApiResponse.ok("成功", null) : ApiResponse.fail("插件未安装");
    }
}
