package com.mapay.service;

import com.mapay.common.SignUtils;
import com.mapay.model.PayAccount;
import com.mapay.model.PayChannel;
import com.mapay.model.PaymentOrder;
import com.mapay.model.User;
import com.mapay.repository.PayAccountRepository;
import com.mapay.repository.PayChannelRepository;
import com.mapay.repository.PaymentOrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LegacyViewService {
    private final AuthService auth;
    private final PayAccountRepository accounts;
    private final PayChannelRepository channels;
    private final PaymentOrderRepository orders;

    public LegacyViewService(AuthService auth, PayAccountRepository accounts, PayChannelRepository channels,
                             PaymentOrderRepository orders) {
        this.auth = auth;
        this.accounts = accounts;
        this.channels = channels;
        this.orders = orders;
    }

    public String render(String viewPath, HttpServletRequest request, Map<String, Object> model) {
        try {
            String html = Files.readString(Path.of("view", viewPath + ".html"), StandardCharsets.UTF_8);
            html = html.replace("/doc.html", "/doc");
            html = html.replace("name=\"channel\" autocomplete=\"off\" class=\"layui-input\" disabled",
                    "name=\"channel\" autocomplete=\"off\" class=\"layui-input\" readonly");
            for (Map.Entry<String, Object> entry : model.entrySet()) {
                String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
                html = html.replace("{$" + entry.getKey() + "}", value);
                html = html.replace("{:" + entry.getKey() + "}", value);
                html = html.replace("<?php echo $" + entry.getKey() + " ?>", value);
            }
            html = applyKnownValues(html, request, model);
            html = filterChannelOptions(html, model);
            html = stripPhp(html);
            html = cleanupLeftoverPhpText(html);
            return html;
        } catch (IOException e) {
            throw new IllegalArgumentException("页面不存在: " + viewPath);
        }
    }

    public Map<String, Object> userModel(HttpServletRequest request) {
        User user = auth.requireUser(request.getSession());
        String domain = domain(request);
        String sign = SignUtils.md5(user.getPid() + user.getSecretKey());
        return Map.of(
                "url", domain,
                "id", user.getId(),
                "pid", user.getPid(),
                "nickname", safe(user.getNickname()),
                "username", safe(user.getUsername()),
                "secret_key", safe(user.getSecretKey()),
                "submiturl", domain + "/submit",
                "apiurl", domain + "/mapi",
                "notifyurl", domain + "/mpayNotify",
                "orderurl", domain + "/checkOrder/" + user.getPid() + "/" + sign
        );
    }

    public Map<String, Object> orderDetailModel(Long id) {
        PaymentOrder order = orders.findById(id).orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        PayAccount account = order.getAid() == null ? null : accounts.findById(order.getAid()).orElse(null);
        PayChannel channel = order.getCid() == null ? null : channels.findById(order.getCid()).orElse(null);
        return Map.ofEntries(
                Map.entry("id", safe(order.getId())),
                Map.entry("order_id", safe(order.getOrderId())),
                Map.entry("pid", safe(order.getPid())),
                Map.entry("type", safe(order.getType())),
                Map.entry("out_trade_no", safe(order.getOutTradeNo())),
                Map.entry("notify_url", safe(order.getNotifyUrl())),
                Map.entry("return_url", safe(order.getReturnUrl())),
                Map.entry("name", safe(order.getName())),
                Map.entry("money", safe(order.getMoney())),
                Map.entry("really_price", safe(order.getReallyPrice())),
                Map.entry("clientip", safe(order.getClientip())),
                Map.entry("device", safe(order.getDevice())),
                Map.entry("param", safe(order.getParam())),
                Map.entry("state", safe(order.getState())),
                Map.entry("patt", safe(order.getPatt())),
                Map.entry("platform", account == null ? safe(order.getPlatform()) : safe(account.getPlatform())),
                Map.entry("platform_order", safe(order.getPlatformOrder())),
                Map.entry("aid", safe(order.getAid())),
                Map.entry("cid", safe(order.getCid())),
                Map.entry("account", account == null ? "..." : safe(account.getAccount())),
                Map.entry("channel", channel == null ? "..." : safe(channel.getChannel())),
                Map.entry("qrcode", channel == null ? "..." : safe(channel.getQrcode())),
                Map.entry("url_type", channel == null ? "..." : safe(channel.getType())),
                Map.entry("create_time", time(order.getCreateTime())),
                Map.entry("close_time", time(order.getCloseTime())),
                Map.entry("pay_time", time(order.getPayTime()))
        );
    }

    public Map<String, Object> accountModel(Long id) {
        PayAccount account = accounts.findById(id).orElseThrow(() -> new IllegalArgumentException("账号不存在"));
        return Map.of(
                "id", account.getId(),
                "platform", safe(account.getPlatform()),
                "account", safe(account.getAccount()),
                "password", safe(account.getPassword()),
                "state", safe(account.getState()),
                "pattern", safe(account.getPattern()),
                "params", safe(account.getParams())
        );
    }

    public Map<String, Object> channelModel(Long id) {
        PayChannel channel = channels.findById(id).orElseThrow(() -> new IllegalArgumentException("通道不存在"));
        PayAccount account = channels.findById(id).flatMap(c -> accounts.findById(c.getAccountId())).orElse(null);
        return Map.of(
                "cid", channel.getId(),
                "aid", safe(channel.getAccountId()),
                "platform", account == null ? "" : safe(account.getPlatform()),
                "account", account == null ? "" : safe(account.getAccount()),
                "channel", safe(channel.getChannel()),
                "qrcode", safe(channel.getQrcode()),
                "last_time", time(channel.getLastTime()),
                "state", safe(channel.getState()),
                "type", safe(channel.getType())
        );
    }

    public Map<String, Object> dashboardModel() {
        LocalDate today = LocalDate.now();
        BigDecimal todayIncome = sum(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        BigDecimal yesterdayIncome = sum(today.minusDays(1).atStartOfDay(), today.atStartOfDay());
        BigDecimal weekIncome = sum(today.minusDays(today.getDayOfWeek().getValue() - 1L).atStartOfDay(), today.plusDays(1).atStartOfDay());
        BigDecimal monthIncome = sum(today.withDayOfMonth(1).atStartOfDay(), today.plusDays(1).atStartOfDay());
        return Map.of(
                "today_income", todayIncome,
                "yesterday_income", yesterdayIncome,
                "week_income", weekIncome,
                "month_income", monthIncome,
                "servertime", time(LocalDateTime.now())
        );
    }

    private BigDecimal sum(LocalDateTime start, LocalDateTime end) {
        return orders.findByCreateTimeBetweenAndState(start, end, 1).stream()
                .map(PaymentOrder::getReallyPrice)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String applyKnownValues(String html, HttpServletRequest request, Map<String, Object> model) {
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            String key = entry.getKey();
            html = html.replace("<?php echo $" + key + "; ?>", value);
            html = html.replace("<?php echo $" + key + "?>", value);
            html = html.replace("<?php echo htmlentities($" + key + "); ?>", value);
            html = html.replace("<?php echo htmlentities($" + key + ");?>", value);
            html = html.replace("<?php echo htmlentities($" + key + ") ?>", value);
        }
        html = html.replace("<?php echo $version ?>", "V1 SpringBoot");
        html = html.replace("<?php echo $servertime ?>", time(LocalDateTime.now()));
        html = html.replace("<?php echo $today_income ?>", String.valueOf(model.getOrDefault("today_income", "0")));
        html = html.replace("<?php echo $yesterday_income ?>", String.valueOf(model.getOrDefault("yesterday_income", "0")));
        html = html.replace("<?php echo $week_income ?>", String.valueOf(model.getOrDefault("week_income", "0")));
        html = html.replace("<?php echo $month_income ?>", String.valueOf(model.getOrDefault("month_income", "0")));
        html = html.replace("<?php echo $url ?>", String.valueOf(model.getOrDefault("url", domain(request))));
        html = html.replace("<?php echo $pid ?>", String.valueOf(model.getOrDefault("pid", "")));
        html = html.replace("<?php echo $key ?>", String.valueOf(model.getOrDefault("key", "")));
        html = html.replace("<?php echo htmlentities($payclient ?? \"\"); ?>", String.valueOf(model.getOrDefault("payclient", "")));
        html = html.replace("<?php echo $payUrl; ?>", String.valueOf(model.getOrDefault("payUrl", "")));
        html = html.replace("<?php echo htmlentities($id);?>", String.valueOf(model.getOrDefault("id", "")));
        html = html.replace("<?php echo $url . 'mpayNotify' ?>", String.valueOf(model.getOrDefault("notifyurl", domain(request) + "/mpayNotify")));
        html = html.replace("<?php echo date('Ymd-His', time()) ?>", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        html = html.replace("<?php echo date('YmdHis', time()).mt_rand(1000,9999) ?>", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "1000");
        html = html.replace("<?php include_once '../view/tpl/pattern.html'; ?>", "<option value=\"0\">单次监听·被动</option><option value=\"1\">连续监听·主动</option>");
        html = html.replace("<?php echo \"<strong>{$pid}</strong>\" ?>", "<strong>" + model.getOrDefault("pid", "") + "</strong>");
        html = html.replace("<?php echo match ((int)$type) {0 => 'link',1 => 'img',default => ''}; ?>",
                "1".equals(String.valueOf(model.getOrDefault("type", ""))) || "image".equals(String.valueOf(model.getOrDefault("type", ""))) ? "img" : "link");
        html = replacePayWay(html, model);
        html = replaceStatus(html, model);
        html = html.replaceAll("<\\?php[\\s\\S]*?\\?>", "");
        return html;
    }

    private String cleanupLeftoverPhpText(String html) {
        return html
                .replaceAll("echo \\$[A-Za-z_][A-Za-z0-9_]*\\s*;?", "")
                .replaceAll("echo htmlentities\\(\\$[A-Za-z_][A-Za-z0-9_]*\\)\\s*;?", "")
                .replaceAll("\\$[A-Za-z_][A-Za-z0-9_]*", "");
    }

    private String filterChannelOptions(String html, Map<String, Object> model) {
        String platform = String.valueOf(model.getOrDefault("platform", ""));
        if ("alipay".equals(platform)) {
            html = html.replaceAll("(?s)<option value=\"wxpay1\">.*?<option value=\"wxpay5\">店员码</option>", "");
            html = html.replaceAll("(?s)<p><strong>手机监控：</strong>微信.*?</p>\\s*<p><strong>电脑监控：</strong>所有收款码均可监控，需要将收款通知聊天窗口单独拖出来</p>", "");
        } else if ("wxpay".equals(platform)) {
            html = html.replaceAll("(?s)<option value=\"alipay1\">.*?<option value=\"alipay2\">经营码</option>", "");
            html = html.replaceAll("(?s)<p><strong>手机监控：</strong>支付宝.*?</p>\\s*<p><strong>电脑监控：</strong>暂不支持PC端监控，可以选择支付宝账单方式监听收款</p>", "");
        }
        return html;
    }

    private String replacePayWay(String html, Map<String, Object> model) {
        String type = String.valueOf(model.getOrDefault("type", ""));
        String label = switch (type) {
            case "alipay" -> "支付宝";
            case "wxpay" -> "微信支付";
            case "unionpay" -> "云闪付";
            default -> type;
        };
        String icon = switch (type) {
            case "alipay" -> "/static/img/alipay.ico";
            case "wxpay" -> "/static/img/wxpay.ico";
            case "unionpay" -> "/static/img/unionpay.ico";
            default -> "";
        };
        return html.replaceAll("<\\?php \\$payway=\\[[\\s\\S]*?\\?>", "<img src='" + icon + "' width='16'><strong class='" + type + "'>" + label + "</strong>");
    }

    private String replaceStatus(String html, Map<String, Object> model) {
        int state = Integer.parseInt(String.valueOf(model.getOrDefault("state", "0")));
        String closeTime = String.valueOf(model.getOrDefault("close_time", ""));
        try {
            if (state == 0 && LocalDateTime.parse(closeTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).isBefore(LocalDateTime.now())) {
                state = -1;
            }
        } catch (Exception ignored) {
        }
        String label = state == 1 ? "支付成功" : state == -1 ? "订单过期" : "等待支付";
        String color = state == 1 ? "green" : state == -1 ? "gray" : "orange";
        return html.replaceAll("<\\?php\\s+if \\(\\$state[\\s\\S]*?\\?>", "<span class='layui-badge layui-bg-" + color + "'>" + label + "</span>");
    }

    private String stripPhp(String html) {
        return html.replaceAll("<\\?php\\s+if\\s*\\([^?]+\\)\\s*\\{\\s*\\?>", "")
                .replaceAll("<\\?php\\s*}\\s*else\\s*\\{\\s*\\?>", "")
                .replaceAll("<\\?php\\s*}\\s*\\?>", "")
                .replaceAll("<\\?php\\s+foreach\\s*\\([^?]+\\)\\s*\\{\\s*\\?>", "")
                .replaceAll("<\\?php\\s+endforeach;?\\s*\\?>", "");
    }

    private String domain(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }

    private Object safe(Object value) {
        return value == null ? "" : value;
    }

    private String time(LocalDateTime time) {
        return time == null ? "" : time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
