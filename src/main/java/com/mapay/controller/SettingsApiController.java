package com.mapay.controller;

import com.mapay.common.ApiResponse;
import com.mapay.model.MailConfig;
import com.mapay.service.OrderMailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/settings", "/api/Settings"})
@Tag(name = "系统设置", description = "后台系统配置管理，包括新订单邮件提醒配置")
public class SettingsApiController {
    private final OrderMailService orderMailService;

    public SettingsApiController(OrderMailService orderMailService) {
        this.orderMailService = orderMailService;
    }

    @GetMapping("/mail")
    @Operation(summary = "获取邮件提醒配置", description = "读取数据库中的新订单邮件提醒配置；首次访问时会自动创建一条默认关闭的配置。")
    public ApiResponse<MailConfig> mailConfig() {
        return ApiResponse.ok("获取成功", orderMailService.currentConfig());
    }

    @PostMapping("/mail")
    @Operation(summary = "保存邮件提醒配置", description = "管理员在后台保存 SMTP 邮箱、收件邮箱和开启状态等配置。")
    public ApiResponse<MailConfig> saveMail(@RequestBody MailConfig config) {
        return ApiResponse.ok("保存成功", orderMailService.saveConfig(config));
    }

    @PostMapping("/mail/test")
    @Operation(summary = "发送测试邮件", description = "按当前数据库配置发送一封测试邮件，成功或失败都会写入最近发送日志。")
    public ApiResponse<Void> testMail() {
        orderMailService.sendTestMail();
        return ApiResponse.ok("测试邮件已执行，请查看最近发送日志", null);
    }
}
