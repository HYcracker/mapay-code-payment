package com.mapay.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mpay_pay_channel")
@Schema(description = "收款通道")
public class PayChannel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "收款通道主键")
    private Long id;
    @Schema(description = "所属收款账号主键")
    private Long accountId;
    @Schema(description = "通道标识，例如 wxpay1#account 或 alipay1#account")
    private String channel;
    @Schema(description = "二维码类型，0 表示链接，1 表示图片")
    private String type = "url";
    @Schema(description = "二维码链接或图片访问地址")
    private String qrcode;
    @Schema(description = "通道状态，1 启用，0 禁用")
    private Integer state = 1;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "每日限额")
    private BigDecimal dailyLimit = BigDecimal.ZERO;
    @Schema(description = "最近使用时间")
    private LocalDateTime lastTime = LocalDateTime.now();
    @Schema(description = "创建时间")
    private LocalDateTime createTime = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getQrcode() { return qrcode; }
    public void setQrcode(String qrcode) { this.qrcode = qrcode; }
    public Integer getState() { return state; }
    public void setState(Integer state) { this.state = state; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
    public LocalDateTime getLastTime() { return lastTime; }
    public void setLastTime(LocalDateTime lastTime) { this.lastTime = lastTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
