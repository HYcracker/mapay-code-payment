package com.mapay.controller;

import com.mapay.common.ApiResponse;
import com.mapay.common.SignUtils;
import com.mapay.model.User;
import com.mapay.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/install", "/Install"})
@Tag(name = "安装初始化", description = "安装向导、数据库配置检查和管理员初始化")
public class InstallApiController {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public InstallApiController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @PostMapping("/install")
    @Operation(summary = "保存安装数据库配置", description = "请求体字段：host 数据库地址；port 数据库端口；name 数据库名；user 数据库账号；password 数据库密码。当前 Java 版配置以 yml 为准，此接口用于兼容安装页流程。")
    public ApiResponse<Void> install(@RequestBody Map<String, String> body) {
        if (isBlank(body.get("host")) || isBlank(body.get("name")) || isBlank(body.get("user")) || isBlank(body.get("port"))) {
            return ApiResponse.fail("数据库配置不完整");
        }
        return ApiResponse.ok("配置保存成功", null);
    }

    @PostMapping("/init")
    @Operation(summary = "初始化管理员账号", description = "请求体字段：username 管理员账号；password 管理员密码；nickname 管理员昵称。若账号存在则更新密码和资料。")
    public ApiResponse<Void> init(@RequestBody Map<String, String> body) {
        if (isBlank(body.get("username")) || isBlank(body.get("password")) || isBlank(body.get("nickname"))) {
            return ApiResponse.fail("管理员信息不完整");
        }
        User user = users.findByUsername(body.get("username")).orElseGet(User::new);
        user.setPid(user.getPid() == null ? Math.max(users.maxPid() + 1, 1000) : user.getPid());
        user.setUsername(body.get("username"));
        user.setPassword(encoder.encode(body.get("password")));
        user.setNickname(body.get("nickname"));
        user.setRole("admin");
        user.setState(1);
        if (user.getSecretKey() == null || user.getSecretKey().isBlank()) {
            user.setSecretKey(randomKey());
        }
        users.save(user);
        return ApiResponse.ok("安装成功", null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String randomKey() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return SignUtils.md5(HexFormat.of().formatHex(bytes));
    }
}
