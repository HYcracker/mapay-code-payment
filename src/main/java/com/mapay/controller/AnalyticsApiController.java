package com.mapay.controller;

import com.mapay.common.ApiResponse;
import com.mapay.model.PaymentOrder;
import com.mapay.repository.PaymentOrderRepository;
import com.mapay.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/analytics", "/api/Analytics"})
@Tag(name = "数据分析", description = "可视化大屏数据接口：概览、趋势、支付类型分布、时段分布、最新订单、商户排行")
public class AnalyticsApiController {

    private final PaymentOrderRepository orders;
    private final UserRepository users;

    public AnalyticsApiController(PaymentOrderRepository orders, UserRepository users) {
        this.orders = orders;
        this.users = users;
    }

    @GetMapping("/overview")
    @Operation(summary = "概览统计", description = "返回今日/昨日/本月/总计的收入、订单数、成功率、均价等概览数据")
    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        // 今日
        List<PaymentOrder> todayPaid = orders.findByCreateTimeBetweenAndState(todayStart, todayEnd, 1);
        long todayOrders = todayPaid.size();
        BigDecimal todayRevenue = sum(todayPaid);
        long todayTotal = orders.findByCreateTimeBetweenAndState(todayStart, todayEnd, 0).size() + todayOrders;

        // 昨日
        List<PaymentOrder> yesterdayPaid = orders.findByCreateTimeBetweenAndState(yesterdayStart, todayStart, 1);
        long yesterdayOrders = yesterdayPaid.size();
        BigDecimal yesterdayRevenue = sum(yesterdayPaid);

        // 本月
        List<PaymentOrder> monthPaid = orders.findByCreateTimeBetweenAndState(monthStart, todayEnd, 1);
        long monthOrders = monthPaid.size();
        BigDecimal monthRevenue = sum(monthPaid);

        // 总计
        long totalOrders = orders.count();
        List<PaymentOrder> allPaid = orders.findByCreateTimeBetweenAndState(
                LocalDateTime.of(2000, 1, 1, 0, 0), todayEnd, 1);
        BigDecimal totalRevenue = sum(allPaid);

        // 成功率
        long allTotal = orders.findByCreateTimeBetweenAndState(LocalDateTime.of(2000, 1, 1, 0, 0), todayEnd, 0).size()
                + allPaid.size();
        String successRate = allTotal > 0
                ? new BigDecimal(allPaid.size()).multiply(new BigDecimal("100")).divide(new BigDecimal(allTotal), 1, RoundingMode.HALF_UP) + "%"
                : "0%";

        // 均价
        BigDecimal avgPrice = allPaid.isEmpty() ? BigDecimal.ZERO
                : totalRevenue.divide(new BigDecimal(allPaid.size()), 2, RoundingMode.HALF_UP);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayRevenue", todayRevenue);
        result.put("todayOrders", todayOrders);
        result.put("yesterdayRevenue", yesterdayRevenue);
        result.put("yesterdayOrders", yesterdayOrders);
        result.put("monthRevenue", monthRevenue);
        result.put("monthOrders", monthOrders);
        result.put("totalRevenue", totalRevenue);
        result.put("totalOrders", totalOrders);
        result.put("successRate", successRate);
        result.put("avgPrice", avgPrice);
        return result;
    }

    @GetMapping("/trend")
    @Operation(summary = "交易趋势", description = "返回最近 N 天的每日订单数和收入，用于折线/柱状图")
    public Map<String, Object> trend(
            @Parameter(description = "统计天数：7/30/90") @RequestParam(defaultValue = "7") int days) {
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime start = LocalDate.now().minusDays(days).atStartOfDay();

        List<Object[]> rows = orders.dailyStats(start, end);
        Map<String, BigDecimal> dataMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String date = row[0].toString();
            BigDecimal amount = (BigDecimal) row[2];
            dataMap.put(date, amount);
        }

        List<String> dates = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String date = LocalDate.now().minusDays(days - 1 - i).toString();
            dates.add(date);
            revenues.add(dataMap.getOrDefault(date, BigDecimal.ZERO));
            counts.add(0L);
        }
        // 填充订单数
        for (Object[] row : rows) {
            String date = row[0].toString();
            long count = (Long) row[1];
            int idx = dates.indexOf(date);
            if (idx >= 0) {
                counts.set(idx, count);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("revenues", revenues);
        result.put("counts", counts);
        return result;
    }

    @GetMapping("/paymentTypes")
    @Operation(summary = "支付类型分布", description = "返回指定时间范围内的支付类型占比，用于饼图/环形图")
    public Map<String, Object> paymentTypes(
            @Parameter(description = "统计天数：7/30/90") @RequestParam(defaultValue = "7") int days) {
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime start = LocalDate.now().minusDays(days).atStartOfDay();

        List<Object[]> rows = orders.typeStats(start, end);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", typeLabel((String) row[0]));
            item.put("type", row[0]);
            item.put("count", row[1]);
            item.put("amount", row[2]);
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        return result;
    }

    @GetMapping("/hourly")
    @Operation(summary = "时段分布", description = "返回指定时间范围内每小时的订单分布，用于柱状图")
    public Map<String, Object> hourly(
            @Parameter(description = "统计天数：7/30") @RequestParam(defaultValue = "7") int days) {
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime start = LocalDate.now().minusDays(days).atStartOfDay();

        List<Object[]> rows = orders.hourlyStats(start, end);
        Map<Integer, Long> dataMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            int hour = ((Number) row[0]).intValue();
            long count = (Long) row[1];
            dataMap.put(hour, count);
        }

        List<Integer> hours = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hours.add(h);
            counts.add(dataMap.getOrDefault(h, 0L));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hours", hours);
        result.put("counts", counts);
        return result;
    }

    @GetMapping("/recentOrders")
    @Operation(summary = "最新订单", description = "返回最近 N 条已支付订单")
    public Map<String, Object> recentOrders(
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> list = orders.findAllByOrderByIdDesc(PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(order -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("orderId", order.getOrderId());
                    item.put("type", typeLabel(order.getType()));
                    item.put("money", order.getMoney());
                    item.put("reallyPrice", order.getReallyPrice());
                    item.put("state", order.getState());
                    item.put("createTime", order.getCreateTime() != null ? order.getCreateTime().toString() : "");
                    return item;
                })
                .collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("msg", "");
        result.put("data", list);
        return result;
    }

    @GetMapping("/topMerchants")
    @Operation(summary = "商户排行", description = "返回指定时间范围内的商户收入排行")
    public List<Map<String, Object>> topMerchants(
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "统计天数") @RequestParam(defaultValue = "30") int days) {
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime start = LocalDate.now().minusDays(days).atStartOfDay();

        List<PaymentOrder> paid = orders.findByCreateTimeBetweenAndState(start, end, 1);
        Map<Long, List<PaymentOrder>> grouped = paid.stream()
                .collect(Collectors.groupingBy(PaymentOrder::getPid));

        return grouped.entrySet().stream()
                .sorted((a, b) -> sum(b.getValue()).compareTo(sum(a.getValue())))
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("pid", entry.getKey());
                    item.put("orderCount", entry.getValue().size());
                    item.put("amount", sum(entry.getValue()));
                    users.findByPid(entry.getKey()).ifPresentOrElse(
                            user -> item.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername()),
                            () -> item.put("nickname", "未知商户"));
                    return item;
                })
                .collect(Collectors.toList());
    }

    private BigDecimal sum(List<PaymentOrder> list) {
        return list.stream()
                .map(PaymentOrder::getReallyPrice)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String typeLabel(String type) {
        if (type == null) return "未知";
        return switch (type) {
            case "wxpay" -> "微信支付";
            case "alipay" -> "支付宝";
            case "unionpay" -> "云闪付";
            default -> type;
        };
    }
}
