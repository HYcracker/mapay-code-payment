package com.mapay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "后台兼容接口", description = "兼容原 PearAdmin 页面直接调用的菜单和消息接口")
public class LegacyAdminApiController {
    @GetMapping("/Console/menu")
    @Operation(summary = "获取后台菜单兼容接口", description = "原后台页面使用的 /Console/menu 菜单接口。")
    public List<Map<String, Object>> menu() {
        return ConsoleApiController.menuItems();
    }

    @GetMapping("/Console/message")
    @Operation(summary = "获取后台消息兼容接口", description = "原后台页面使用的 /Console/message 消息接口。")
    public List<Map<String, Object>> message() {
        return ConsoleApiController.messageItems();
    }
}
