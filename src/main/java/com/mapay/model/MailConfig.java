package com.mapay.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "mpay_mail_config")
@Schema(description = "新订单邮件提醒配置")
public class MailConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "配置主键")
    private Long id;
    @Schema(description = "是否开启新订单邮件提醒：1 开启，0 关闭")
    private Integer enabled = 0;
    @Schema(description = "邮箱类型：qq、163、126、gmail、outlook、custom")
    @Column(name = "`type`")
    private String type = "custom";
    @Schema(description = "SMTP 服务器地址。custom 类型必填，常见邮箱类型可留空自动使用默认 SMTP")
    private String host = "";
    @Schema(description = "SMTP 端口")
    private Integer port = 465;
    @Schema(description = "SMTP 登录账号")
    private String username = "";
    @Schema(description = "SMTP 授权码或密码")
    @Column(name = "`password`")
    private String password = "";
    @Schema(description = "发件邮箱，留空时使用 SMTP 登录账号")
    @Column(name = "from_email")
    private String from = "";
    @Schema(description = "发件人名称")
    private String fromName = "Mpay";
    @Schema(description = "收件邮箱列表，多个邮箱可用逗号、分号、空格或换行分隔")
    @Column(length = 2000)
    private String toEmails = "";
    @Schema(description = "是否启用 SMTP 认证：1 开启，0 关闭")
    private Integer auth = 1;
    @Schema(description = "是否启用 SSL：1 开启，0 关闭")
    @Column(name = "`ssl`")
    private Integer ssl = 1;
    @Schema(description = "是否启用 STARTTLS：1 开启，0 关闭")
    private Integer starttls = 0;
    @Schema(description = "连接超时时间，单位毫秒")
    private Integer connectionTimeout = 10000;
    @Schema(description = "读取超时时间，单位毫秒")
    @Column(name = "`timeout`")
    private Integer timeout = 10000;
    @Schema(description = "写入超时时间，单位毫秒")
    private Integer writeTimeout = 10000;
    @Schema(description = "邮件标题前缀")
    private String subjectPrefix = "Mpay 新订单提醒";
    @Schema(description = "最近一次发送状态：success 成功，fail 失败")
    private String lastSendStatus = "";
    @Schema(description = "最近一次发送日志")
    @Column(length = 2000)
    private String lastSendMessage = "";
    @Schema(description = "最近一次发送时间")
    private LocalDateTime lastSendTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime = LocalDateTime.now();
    @Schema(description = "更新时间")
    private LocalDateTime updateTime = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public String getToEmails() { return toEmails; }
    public void setToEmails(String toEmails) { this.toEmails = toEmails; }
    public Integer getAuth() { return auth; }
    public void setAuth(Integer auth) { this.auth = auth; }
    public Integer getSsl() { return ssl; }
    public void setSsl(Integer ssl) { this.ssl = ssl; }
    public Integer getStarttls() { return starttls; }
    public void setStarttls(Integer starttls) { this.starttls = starttls; }
    public Integer getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(Integer connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    public Integer getTimeout() { return timeout; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }
    public Integer getWriteTimeout() { return writeTimeout; }
    public void setWriteTimeout(Integer writeTimeout) { this.writeTimeout = writeTimeout; }
    public String getSubjectPrefix() { return subjectPrefix; }
    public void setSubjectPrefix(String subjectPrefix) { this.subjectPrefix = subjectPrefix; }
    public String getLastSendStatus() { return lastSendStatus; }
    public void setLastSendStatus(String lastSendStatus) { this.lastSendStatus = lastSendStatus; }
    public String getLastSendMessage() { return lastSendMessage; }
    public void setLastSendMessage(String lastSendMessage) { this.lastSendMessage = lastSendMessage; }
    public LocalDateTime getLastSendTime() { return lastSendTime; }
    public void setLastSendTime(LocalDateTime lastSendTime) { this.lastSendTime = lastSendTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
