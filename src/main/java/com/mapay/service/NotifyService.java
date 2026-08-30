package com.mapay.service;

import com.mapay.common.SignUtils;
import com.mapay.model.PaymentOrder;
import com.mapay.model.User;
import com.mapay.repository.UserRepository;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NotifyService {
    private final UserRepository users;
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    public NotifyService(UserRepository users) {
        this.users = users;
    }

    public Map<String, Object> notifyParams(PaymentOrder order) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("pid", order.getPid());
        params.put("trade_no", order.getOrderId());
        params.put("out_trade_no", order.getOutTradeNo());
        params.put("type", order.getType());
        params.put("name", order.getName());
        params.put("money", order.getMoney());
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("sign_type", "MD5");
        if (StringUtils.hasText(order.getParam())) {
            params.put("param", order.getParam());
        }
        User user = users.findByPid(order.getPid()).orElseThrow();
        params.put("sign", SignUtils.sign(params, user.getSecretKey()));
        return params;
    }

    public boolean send(PaymentOrder order) {
        if (!StringUtils.hasText(order.getNotifyUrl())) {
            return false;
        }
        try {
            String url = appendQuery(order.getNotifyUrl(), notifyParams(order));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().timeout(java.time.Duration.ofSeconds(10)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return "success".equalsIgnoreCase(response.body().trim());
        } catch (Exception ignored) {
            return false;
        }
    }

    public String returnUrl(PaymentOrder order) {
        if (!StringUtils.hasText(order.getReturnUrl())) {
            return "";
        }
        return appendQuery(order.getReturnUrl(), notifyParams(order));
    }

    private static String appendQuery(String url, Map<String, Object> params) {
        StringBuilder builder = new StringBuilder(url);
        builder.append(url.contains("?") ? '&' : '?');
        params.forEach((key, value) -> {
            if (value != null) {
                builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8))
                        .append('&');
            }
        });
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }
}
