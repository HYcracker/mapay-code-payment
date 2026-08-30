package com.mapay.service;

import com.mapay.common.ApiResponse;
import com.mapay.common.SignUtils;
import com.mapay.model.User;
import com.mapay.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    public static final String SESSION_USER_ID = "userid";
    public static final String SESSION_PID = "pid";
    public static final String SESSION_NICKNAME = "nickname";
    public static final String SESSION_ROLE = "userrole";

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public ApiResponse<Void> login(String username, String password, HttpSession session) {
        return users.findByUsername(username)
                .filter(user -> encoder.matches(password, user.getPassword()))
                .map(user -> {
                    session.setAttribute(SESSION_USER_ID, user.getId());
                    session.setAttribute(SESSION_PID, user.getPid());
                    session.setAttribute(SESSION_NICKNAME, user.getNickname());
                    session.setAttribute(SESSION_ROLE, user.getRole());
                    return ApiResponse.<Void>ok("ok", null);
                })
                .orElseGet(() -> ApiResponse.fail("用户名或密码错误"));
    }

    public Long requirePid(HttpSession session) {
        Object pid = session.getAttribute(SESSION_PID);
        if (pid == null) {
            throw new IllegalStateException("未登录");
        }
        return Long.valueOf(String.valueOf(pid));
    }

    public User requireUser(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);
        if (id == null) {
            throw new IllegalStateException("未登录");
        }
        return users.findById(Long.valueOf(String.valueOf(id))).orElseThrow(() -> new IllegalStateException("用户不存在"));
    }

    public String resetKey(User user) {
        user.setSecretKey(randomKey());
        users.save(user);
        return user.getSecretKey();
    }

    public String randomKey() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return SignUtils.md5(HexFormat.of().formatHex(bytes));
    }
}
