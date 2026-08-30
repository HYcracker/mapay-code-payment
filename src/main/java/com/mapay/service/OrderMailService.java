package com.mapay.service;

import com.mapay.config.MapayProperties;
import com.mapay.model.MailConfig;
import com.mapay.model.PaymentOrder;
import com.mapay.repository.MailConfigRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderMailService {
    private static final Logger log = LoggerFactory.getLogger(OrderMailService.class);

    private final MailConfigRepository configs;
    private final MapayProperties properties;

    public OrderMailService(MailConfigRepository configs, MapayProperties properties) {
        this.configs = configs;
        this.properties = properties;
    }

    @Transactional
    public MailConfig currentConfig() {
        return configs.findFirstByOrderByIdAsc().orElseGet(() -> {
            MailConfig config = new MailConfig();
            config.setEnabled(0);
            return configs.save(config);
        });
    }

    @Transactional
    public MailConfig saveConfig(MailConfig config) {
        MailConfig current = currentConfig();
        current.setEnabled(oneOrZero(config.getEnabled()));
        current.setType(defaultString(config.getType(), "custom"));
        current.setHost(defaultString(config.getHost(), ""));
        current.setPort(defaultInt(config.getPort(), 465));
        current.setUsername(defaultString(config.getUsername(), ""));
        current.setPassword(defaultString(config.getPassword(), ""));
        current.setFrom(defaultString(config.getFrom(), ""));
        current.setFromName(defaultString(config.getFromName(), "Mpay"));
        current.setToEmails(defaultString(config.getToEmails(), ""));
        current.setAuth(oneOrZero(defaultInt(config.getAuth(), 1)));
        current.setSsl(oneOrZero(defaultInt(config.getSsl(), 1)));
        current.setStarttls(oneOrZero(defaultInt(config.getStarttls(), 0)));
        current.setConnectionTimeout(defaultInt(config.getConnectionTimeout(), 10000));
        current.setTimeout(defaultInt(config.getTimeout(), 10000));
        current.setWriteTimeout(defaultInt(config.getWriteTimeout(), 10000));
        current.setSubjectPrefix(defaultString(config.getSubjectPrefix(), "Mpay 新订单提醒"));
        current.setUpdateTime(LocalDateTime.now());
        return configs.save(current);
    }

    @Async
    public void notifyNewOrder(PaymentOrder order, String rawToken) {
        MailConfig config = currentConfig();
        if (!enabled(config)) {
            log.info("新订单邮件提醒未开启，订单号：{}", order.getOrderId());
            return;
        }
        sendWithLog(config, subject(config, order), body(order, rawToken), "新订单提醒：" + order.getOrderId());
    }

    public void sendTestMail() {
        MailConfig config = currentConfig();
        sendWithLog(config,
                defaultString(config.getSubjectPrefix(), "Mpay 新订单提醒") + " - 测试邮件",
                "这是一封测试邮件。收到此邮件说明当前 SMTP 邮箱配置可用。",
                "测试邮件");
    }

    private void sendWithLog(MailConfig config, String subject, String text, String scene) {
        try {
            validate(config);
            JavaMailSenderImpl sender = sender(config);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from(config));
            message.setTo(recipients(config).toArray(String[]::new));
            message.setSubject(subject);
            message.setText(text);
            sender.send(message);
            record(config, "success", scene + "发送成功");
            log.info("{}发送成功", scene);
        } catch (Exception e) {
            record(config, "fail", scene + "发送失败：" + rootMessage(e));
            log.warn("{}发送失败", scene, e);
        }
    }

    private JavaMailSenderImpl sender(MailConfig config) {
        MailServer server = resolveServer(config);
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(server.host());
        sender.setPort(server.port());
        sender.setUsername(config.getUsername());
        sender.setPassword(config.getPassword());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(enabled(config.getAuth())));
        props.put("mail.smtp.ssl.enable", String.valueOf(server.ssl()));
        props.put("mail.smtp.starttls.enable", String.valueOf(server.starttls()));
        props.put("mail.smtp.connectiontimeout", String.valueOf(defaultInt(config.getConnectionTimeout(), 10000)));
        props.put("mail.smtp.timeout", String.valueOf(defaultInt(config.getTimeout(), 10000)));
        props.put("mail.smtp.writetimeout", String.valueOf(defaultInt(config.getWriteTimeout(), 10000)));
        return sender;
    }

    private MailServer resolveServer(MailConfig config) {
        String host = blankToNull(config.getHost());
        int port = defaultInt(config.getPort(), 465);
        boolean ssl = enabled(config.getSsl());
        boolean starttls = enabled(config.getStarttls());
        if (host != null) {
            return new MailServer(host, port, ssl, starttls);
        }

        return switch ((config.getType() == null ? "custom" : config.getType()).toLowerCase(Locale.ROOT)) {
            case "qq" -> new MailServer("smtp.qq.com", 465, true, false);
            case "163" -> new MailServer("smtp.163.com", 465, true, false);
            case "126" -> new MailServer("smtp.126.com", 465, true, false);
            case "gmail" -> new MailServer("smtp.gmail.com", 587, false, true);
            case "outlook", "hotmail" -> new MailServer("smtp.office365.com", 587, false, true);
            default -> new MailServer("", port, ssl, starttls);
        };
    }

    private void validate(MailConfig config) {
        MailServer server = resolveServer(config);
        if (server.host().isBlank()) {
            throw new IllegalArgumentException("未配置 SMTP 服务器地址");
        }
        if (blankToNull(config.getUsername()) == null) {
            throw new IllegalArgumentException("未配置发件邮箱账号");
        }
        if (blankToNull(config.getPassword()) == null && enabled(config.getAuth())) {
            throw new IllegalArgumentException("未配置邮箱授权码或密码");
        }
        if (recipients(config).isEmpty()) {
            throw new IllegalArgumentException("未配置收件邮箱");
        }
    }

    private List<String> recipients(MailConfig config) {
        String toEmails = defaultString(config.getToEmails(), "");
        return Arrays.stream(toEmails.split("[,，;；\\s]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private String from(MailConfig config) {
        String from = blankToNull(config.getFrom());
        return from == null ? config.getUsername() : from;
    }

    private String subject(MailConfig config, PaymentOrder order) {
        return defaultString(config.getSubjectPrefix(), "Mpay 新订单提醒") + " - " + order.getOutTradeNo();
    }

    private String body(PaymentOrder order, String rawToken) {
        String confirmUrl = properties.getBaseUrl() + "/confirm/" + rawToken;
        return """
                有新的支付订单，请及时处理。

                平台订单号：%s
                商户订单号：%s
                商户 PID：%s
                支付方式：%s
                商品名称：%s
                订单金额：%s
                实付金额：%s
                订单状态：%s
                创建时间：%s
                关闭时间：%s
                异步通知地址：%s
                同步跳转地址：%s

                管理员确认链接：%s

                点击此链接可查看订单详情并确认收款。确认后系统将自动回调订单。
                """.formatted(
                safe(order.getOrderId()),
                safe(order.getOutTradeNo()),
                safe(order.getPid()),
                safe(order.getType()),
                safe(order.getName()),
                safe(order.getMoney()),
                safe(order.getReallyPrice()),
                safe(order.getState()),
                safe(order.getCreateTime()),
                safe(order.getCloseTime()),
                safe(order.getNotifyUrl()),
                safe(order.getReturnUrl()),
                confirmUrl
        );
    }

    private void record(MailConfig config, String status, String message) {
        try {
            MailConfig current = configs.findById(config.getId()).orElse(config);
            current.setLastSendStatus(status);
            current.setLastSendMessage(message.length() > 2000 ? message.substring(0, 2000) : message);
            current.setLastSendTime(LocalDateTime.now());
            current.setUpdateTime(LocalDateTime.now());
            configs.save(current);
        } catch (Exception e) {
            log.warn("记录邮件发送日志失败", e);
        }
    }

    private String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() == null || root.getMessage().isBlank() ? root.getClass().getSimpleName() : root.getMessage();
    }

    private boolean enabled(MailConfig config) {
        return config != null && enabled(config.getEnabled());
    }

    private boolean enabled(Integer value) {
        return value != null && value == 1;
    }

    private int oneOrZero(Integer value) {
        return enabled(value) ? 1 : 0;
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record MailServer(String host, int port, boolean ssl, boolean starttls) {
    }
}
