package com.mapay.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class PluginService {
    private final Map<String, Boolean> installed = new ConcurrentHashMap<>();
    private final Map<String, Integer> states = new ConcurrentHashMap<>();

    public PluginService() {
        for (Map<String, Object> plugin : catalog()) {
            String platform = String.valueOf(plugin.get("platform"));
            installed.put(platform, true);
            states.put(platform, 1);
        }
    }

    public List<Map<String, Object>> list() {
        return list(1);
    }

    public List<Map<String, Object>> list(int show) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> plugin : catalog()) {
            String platform = String.valueOf(plugin.get("platform"));
            boolean isInstall = installed.getOrDefault(platform, false);
            if (show == 1 && !isInstall) continue;
            if (show == 2 && isInstall) continue;
            rows.add(Map.ofEntries(
                    Map.entry("platform", platform),
                    Map.entry("name", plugin.get("name")),
                    Map.entry("class_name", plugin.get("class_name")),
                    Map.entry("price", plugin.get("price")),
                    Map.entry("describe", plugin.get("describe")),
                    Map.entry("website", plugin.get("website")),
                    Map.entry("helplink", plugin.get("helplink")),
                    Map.entry("version", plugin.get("version")),
                    Map.entry("install", isInstall),
                    Map.entry("state", states.getOrDefault(platform, 1))
            ));
        }
        return rows;
    }

    public List<Map<String, Object>> options() {
        return list(1).stream()
                .filter(item -> Integer.valueOf(1).equals(item.get("state")))
                .map(item -> Map.of("platform", item.get("platform"), "name", item.get("name")))
                .toList();
    }

    public boolean install(String platform) {
        installed.put(platform, true);
        states.put(platform, 1);
        return true;
    }

    public boolean uninstall(String platform) {
        installed.put(platform, false);
        states.put(platform, 0);
        return true;
    }

    public boolean enable(String platform, int state) {
        if (!installed.getOrDefault(platform, false)) {
            return false;
        }
        states.put(platform, state);
        return true;
    }

    private List<Map<String, Object>> catalog() {
        return List.of(
                Map.of("platform", "manual", "name", "通用收款", "class_name", "ManualPay", "price", 0, "describe", "通用静态二维码或链接收款，适合手动确认和测试。", "website", "https://gitee.com/HYcracker/mapay-code-payment", "helplink", "https://gitee.com/HYcracker/mapay-code-payment", "version", "1.0.0"),
                Map.of("platform", "wxpay", "name", "微信支付", "class_name", "WxPay", "price", 0, "describe", "微信个人收款码监听，兼容原 mpay 通知格式。", "website", "https://pay.weixin.qq.com/", "helplink", "https://gitee.com/HYcracker/mapay-code-payment", "version", "1.0.0"),
                Map.of("platform", "alipay", "name", "支付宝", "class_name", "AliPay", "price", 0, "describe", "支付宝个人收款码监听，支持图片码和支付链接。", "website", "https://www.alipay.com/", "helplink", "https://gitee.com/HYcracker/mapay-code-payment", "version", "1.0.0"),
                Map.of("platform", "sqbpay", "name", "收钱吧", "class_name", "SqbPay", "price", 0, "describe", "聚合码收款平台适配，保留主动查询入口。", "website", "https://www.shouqianba.com/", "helplink", "https://gitee.com/HYcracker/mapay-code-payment", "version", "1.0.0")
        );
    }
}
