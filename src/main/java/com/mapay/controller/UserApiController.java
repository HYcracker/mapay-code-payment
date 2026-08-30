package com.mapay.controller;

import com.mapay.common.ApiResponse;
import com.mapay.model.User;
import com.mapay.repository.UserRepository;
import com.mapay.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/user", "/api/User"})
@Tag(name = "用户中心", description = "后台登录、退出、基本资料、密码和商户密钥管理")
public class UserApiController {
    private final AuthService auth;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserApiController(AuthService auth, UserRepository users, PasswordEncoder encoder) {
        this.auth = auth;
        this.users = users;
        this.encoder = encoder;
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "请求体字段：username 登录账号；password 登录密码。登录成功后写入后台会话。")
    public ApiResponse<Void> login(@RequestBody Map<String, String> body, HttpSession session) {
        return auth.login(body.get("username"), body.get("password"), session);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "清除当前后台登录会话。")
    public ApiResponse<Void> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.ok("注销成功", null);
    }

    @GetMapping("/logout")
    @Operation(summary = "退出登录 GET 兼容接口", description = "兼容原 PHP 后台的 GET 退出请求。")
    public ApiResponse<Void> logoutGet(HttpSession session) {
        return logout(session);
    }

    @PostMapping("/profile")
    @Operation(summary = "获取当前用户资料", description = "返回当前登录用户的 PID、账号、昵称、角色、状态和商户密钥等资料。")
    public ApiResponse<User> profile(HttpSession session) {
        return ApiResponse.ok("OK", auth.requireUser(session));
    }

    @PostMapping("/editUser")
    @Operation(summary = "修改基本资料", description = "请求体字段：nickname 昵称。当前接口只允许修改当前登录用户的基本资料。")
    public ApiResponse<Void> editUser(@RequestBody Map<String, String> body, HttpSession session) {
        User user = auth.requireUser(session);
        user.setNickname(body.getOrDefault("nickname", user.getNickname()));
        users.save(user);
        return ApiResponse.ok("修改成功", null);
    }

    @PostMapping("/changePassword")
    @Operation(summary = "修改登录密码", description = "请求体字段：old_password 原密码；new_password 新密码。")
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> body, HttpSession session) {
        User user = auth.requireUser(session);
        if (!encoder.matches(body.get("old_password"), user.getPassword())) {
            return ApiResponse.fail("原密码错误");
        }
        user.setPassword(encoder.encode(body.get("new_password")));
        users.save(user);
        return ApiResponse.ok("修改成功", null);
    }

    @PostMapping("/resetKey")
    @Operation(summary = "重置商户密钥", description = "重新生成当前商户的接口签名密钥，旧密钥会立即失效。")
    public ApiResponse<String> resetKey(HttpSession session) {
        return ApiResponse.ok("重置成功", auth.resetKey(auth.requireUser(session)));
    }

    @GetMapping("/resetKey")
    @Operation(summary = "重置商户密钥 GET 兼容接口", description = "兼容原后台页面的 GET 调用方式。")
    public ApiResponse<String> resetKeyGet(HttpSession session) {
        return resetKey(session);
    }
}
