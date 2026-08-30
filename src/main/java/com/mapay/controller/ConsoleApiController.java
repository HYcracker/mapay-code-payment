package com.mapay.controller;

import com.mapay.model.PaymentOrder;
import com.mapay.repository.PaymentOrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/console", "/api/Console"})
@Tag(name = "后台首页", description = "后台菜单、消息入口和首页统计数据")
public class ConsoleApiController {
    private final PaymentOrderRepository orders;

    public ConsoleApiController(PaymentOrderRepository orders) {
        this.orders = orders;
    }

    @GetMapping("/menu")
    @Operation(summary = "获取后台菜单", description = "返回 PearAdmin 后台左侧菜单结构。")
    public List<Map<String, Object>> menu() {
        return menuItems();
    }

    static List<Map<String, Object>> menuItems() {
        return List.of(
                Map.of("id", "console", "title", "首页", "type", 1, "icon", "layui-icon layui-icon-console", "href", "/Console/console"),
                Map.of("id", "pay", "title", "收款管理", "type", 0, "icon", "layui-icon layui-icon-rmb", "children", List.of(
                        Map.of("id", "payManage", "title", "收款账号", "type", 1, "href", "/PayManage/index"),
                        Map.of("id", "payStatistics", "title", "收款统计", "type", 1, "href", "/PayManage/payStatistics")
                )),
                Map.of("id", "order", "title", "订单管理", "type", 1, "icon", "layui-icon layui-icon-form", "href", "/Order/index"),
                Map.of("id", "analytics", "title", "数据分析", "type", 1, "icon", "layui-icon layui-icon-chart", "href", "/Analytics/index"),
                Map.of("id", "plugin", "title", "插件中心", "type", 1, "icon", "layui-icon layui-icon-app", "href", "/Plugin/index"),
                Map.of("id", "settings", "title", "系统设置", "type", 0, "icon", "layui-icon layui-icon-set", "children", List.of(
                        Map.of("id", "mailSettings", "title", "邮件提醒", "type", 1, "href", "/Settings/mail")
                )),
                Map.of("id", "userCenter", "title", "基本资料", "type", 1, "icon", "layui-icon layui-icon-user", "href", "/User/index")
        );
    }

    @GetMapping("/message")
    @Operation(summary = "获取消息分组", description = "返回后台右上角消息下拉框的分类数据。")
    public List<Map<String, Object>> message() {
        return messageItems();
    }

    static List<Map<String, Object>> messageItems() {
        return List.of(
                Map.of("id", 1, "title", "应用更新", "children", List.of()),
                Map.of("id", 2, "title", "官方消息", "children", List.of())
        );
    }

    @GetMapping("/orderinfo")
    @Operation(summary = "首页收款统计", description = "按时间范围统计订单数量、总金额，以及微信、支付宝、银联的订单数和金额。")
    public Map<String, Object> orderInfo(@Parameter(description = "统计范围：0 今年；1 最近 30 天；2 最近 6 个月；3 最近 1 年")
                                         @RequestParam(defaultValue = "0") int time) {
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);
        LocalDateTime start = switch (time) {
            case 1 -> LocalDateTime.now().minusDays(30);
            case 2 -> LocalDateTime.now().minusMonths(6);
            case 3 -> LocalDateTime.now().minusYears(1);
            default -> LocalDate.now().withDayOfYear(1).atStartOfDay();
        };
        List<PaymentOrder> paid = orders.findByCreateTimeBetweenAndState(start, end, 1);
        return Map.of(
                "ordernum", paid.size(),
                "totalmoney", sum(paid),
                "wxpay", bucket(paid, "wxpay"),
                "alipay", bucket(paid, "alipay"),
                "unionpay", bucket(paid, "unionpay")
        );
    }

    private Map<String, Object> bucket(List<PaymentOrder> orders, String type) {
        List<PaymentOrder> filtered = orders.stream().filter(order -> type.equals(order.getType())).toList();
        return Map.of("num", filtered.size(), "money", sum(filtered));
    }

    private BigDecimal sum(List<PaymentOrder> data) {
        return data.stream().map(PaymentOrder::getReallyPrice).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
