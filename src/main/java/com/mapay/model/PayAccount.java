package com.mapay.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Entity
@Table(name = "mpay_pay_account")
@Schema(description = "收款账号")
public class PayAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "收款账号主键")
    private Long id;
    @Schema(description = "所属商户 PID")
    private Long pid;
    @Schema(description = "支付平台，如 wxpay、alipay、manual")
    private String platform;
    @Schema(description = "账号名称或收款账号标识")
    private String account;
    @Schema(description = "账号密码或扩展凭据")
    private String password;
    @Schema(description = "账号状态，1 启用，0 禁用")
    private Integer state = 1;
    @Schema(description = "收款模式，沿用原项目约定")
    private Integer pattern = 1;
    @Schema(description = "扩展参数 JSON")
    private String params = "{}";
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPid() { return pid; }
    public void setPid(Long pid) { this.pid = pid; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getState() { return state; }
    public void setState(Integer state) { this.state = state; }
    public Integer getPattern() { return pattern; }
    public void setPattern(Integer pattern) { this.pattern = pattern; }
    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
