package com.mapay.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mpay_order")
@Schema(description = "支付订单")
public class PaymentOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "订单主键")
    private Long id;
    @Column(unique = true, nullable = false)
    @Schema(description = "平台订单号")
    private String orderId;
    @Schema(description = "商户 PID")
    private Long pid;
    @Schema(description = "支付类型，如 wxpay、alipay、unionpay")
    private String type;
    @Column(unique = true)
    @Schema(description = "商户订单号")
    private String outTradeNo;
    @Schema(description = "异步通知地址")
    private String notifyUrl;
    @Schema(description = "支付完成同步跳转地址")
    private String returnUrl;
    @Schema(description = "商品名称")
    private String name;
    @Schema(description = "订单金额")
    private BigDecimal money;
    @Schema(description = "实际支付金额")
    private BigDecimal reallyPrice;
    @Schema(description = "客户端 IP")
    private String clientip;
    @Schema(description = "客户端设备类型")
    private String device;
    @Column(length = 2000)
    @Schema(description = "商户透传参数")
    private String param;
    @Schema(description = "订单状态，0 等待支付，1 已支付")
    private Integer state = 0;
    @Schema(description = "支付检测状态，沿用原项目约定")
    private Integer patt = 1;
    @Schema(description = "实际使用的收款平台")
    private String platform;
    @Schema(description = "第三方平台流水号")
    private String platformOrder;
    @Schema(description = "收款账号主键")
    private Long aid;
    @Schema(description = "收款通道主键")
    private Long cid;
    @Schema(description = "创建时间")
    private LocalDateTime createTime = LocalDateTime.now();
    @Schema(description = "订单关闭时间")
    private LocalDateTime closeTime;
    @Schema(description = "支付完成时间")
    private LocalDateTime payTime;
    @Column(unique = true)
    @Schema(description = "管理员确认回调令牌（SHA-256 哈希存储）")
    private String confirmToken;
    @Schema(description = "管理员确认时的 IP 地址")
    private String confirmIp;
    @Schema(description = "管理员确认时间")
    private LocalDateTime confirmTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getPid() { return pid; }
    public void setPid(Long pid) { this.pid = pid; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getOutTradeNo() { return outTradeNo; }
    public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
    public String getNotifyUrl() { return notifyUrl; }
    public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getMoney() { return money; }
    public void setMoney(BigDecimal money) { this.money = money; }
    public BigDecimal getReallyPrice() { return reallyPrice; }
    public void setReallyPrice(BigDecimal reallyPrice) { this.reallyPrice = reallyPrice; }
    public String getClientip() { return clientip; }
    public void setClientip(String clientip) { this.clientip = clientip; }
    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
    public String getParam() { return param; }
    public void setParam(String param) { this.param = param; }
    public Integer getState() { return state; }
    public void setState(Integer state) { this.state = state; }
    public Integer getPatt() { return patt; }
    public void setPatt(Integer patt) { this.patt = patt; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getPlatformOrder() { return platformOrder; }
    public void setPlatformOrder(String platformOrder) { this.platformOrder = platformOrder; }
    public Long getAid() { return aid; }
    public void setAid(Long aid) { this.aid = aid; }
    public Long getCid() { return cid; }
    public void setCid(Long cid) { this.cid = cid; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalDateTime closeTime) { this.closeTime = closeTime; }
    public LocalDateTime getPayTime() { return payTime; }
    public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }
    public String getConfirmToken() { return confirmToken; }
    public void setConfirmToken(String confirmToken) { this.confirmToken = confirmToken; }
    public String getConfirmIp() { return confirmIp; }
    public void setConfirmIp(String confirmIp) { this.confirmIp = confirmIp; }
    public LocalDateTime getConfirmTime() { return confirmTime; }
    public void setConfirmTime(LocalDateTime confirmTime) { this.confirmTime = confirmTime; }
}
