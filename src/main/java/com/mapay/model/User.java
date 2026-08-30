package com.mapay.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Entity
@Table(name = "mpay_user")
@Schema(description = "商户/管理员用户")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "用户主键")
    private Long id;
    @Column(unique = true, nullable = false)
    @Schema(description = "商户 PID，用于接口下单和签名")
    private Long pid;
    @Column(unique = true, nullable = false, length = 80)
    @Schema(description = "登录账号")
    private String username;
    @Column(nullable = false)
    @Schema(description = "登录密码密文")
    private String password;
    @Column(nullable = false)
    @Schema(description = "商户接口密钥")
    private String secretKey;
    @Schema(description = "用户昵称")
    private String nickname;
    @Schema(description = "用户角色")
    private String role = "user";
    @Schema(description = "账号状态，1 启用，0 禁用")
    private Integer state = 1;
    @Schema(description = "创建时间")
    private LocalDateTime createTime = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPid() { return pid; }
    public void setPid(Long pid) { this.pid = pid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getState() { return state; }
    public void setState(Integer state) { this.state = state; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
