package com.mapay.controller;

import com.mapay.common.ApiResponse;
import com.mapay.common.PageResponse;
import com.mapay.model.PaymentOrder;
import com.mapay.repository.PaymentOrderRepository;
import com.mapay.service.NotifyService;
import com.mapay.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/order", "/api/Order"})
@Tag(name = "订单管理", description = "后台订单列表、补单、删除、重新通知和超时订单清理")
public class OrderApiController {
    private final PaymentOrderRepository orders;
    private final OrderService orderService;
    private final NotifyService notifyService;

    public OrderApiController(PaymentOrderRepository orders, OrderService orderService, NotifyService notifyService) {
        this.orders = orders;
        this.orderService = orderService;
        this.notifyService = notifyService;
    }

    @GetMapping("/getOrders")
    @Operation(summary = "查询订单列表", description = "支持按 order_id、out_trade_no、name、type、platform、aid、cid、pid、state 模糊筛选，返回 Layui 表格分页格式。")
    public PageResponse<PaymentOrder> getOrders(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                                @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int limit,
                                                @RequestParam Map<String, String> query) {
        List<PaymentOrder> filtered = orders.findAll().stream()
                .filter(order -> match(order.getOrderId(), query.get("order_id")))
                .filter(order -> match(order.getOutTradeNo(), query.get("out_trade_no")))
                .filter(order -> match(order.getName(), query.get("name")))
                .filter(order -> match(order.getType(), query.get("type")))
                .filter(order -> match(order.getPlatform(), query.get("platform")))
                .filter(order -> match(order.getAid(), query.get("aid")))
                .filter(order -> match(order.getCid(), query.get("cid")))
                .filter(order -> match(order.getPid(), query.get("pid")))
                .filter(order -> match(order.getState(), query.get("state")))
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .toList();
        int from = Math.max(page - 1, 0) * limit;
        List<PaymentOrder> rows = filtered.stream().skip(from).limit(limit).toList();
        return PageResponse.of(filtered.size(), rows);
    }

    @PostMapping("/changeOrderState")
    @Operation(summary = "修改订单状态", description = "请求体字段：id 订单主键；state 订单状态，0 等待支付，1 已支付，其他值按后台页面约定。")
    public ApiResponse<PaymentOrder> changeOrderState(@RequestBody Map<String, String> body) {
        PaymentOrder order = orders.findById(Long.valueOf(body.get("id"))).orElseThrow();
        order.setState(Integer.valueOf(body.get("state")));
        return ApiResponse.ok("修改成功", orders.save(order));
    }

    @PostMapping("/doPayOrder")
    @Operation(summary = "手动补单", description = "请求体字段：id 订单主键；platform_order 可选，第三方流水号。补单后会尝试发送异步通知。")
    public ApiResponse<PaymentOrder> doPayOrder(@RequestBody Map<String, String> body) {
        PaymentOrder order = orderService.markPaid(Long.valueOf(body.get("id")), body.getOrDefault("platform_order", "manual"));
        return ApiResponse.ok("补单成功，已尝试异步通知", order);
    }

    @PostMapping("/redoPayOrder")
    @Operation(summary = "重新发送异步通知", description = "请求体字段：id 订单主键。按订单 notify_url 再次通知业务系统。")
    public ApiResponse<Void> redoPayOrder(@RequestBody Map<String, String> body) {
        PaymentOrder order = orders.findById(Long.valueOf(body.get("id"))).orElseThrow();
        return notifyService.send(order) ? ApiResponse.ok("订单通知成功", null) : ApiResponse.fail("异步通知失败");
    }

    @PostMapping("/deleteOrder")
    @Operation(summary = "删除单个订单", description = "请求体字段：id 订单主键。")
    public ApiResponse<Void> deleteOrder(@RequestBody Map<String, String> body) {
        orders.deleteById(Long.valueOf(body.get("id")));
        return ApiResponse.ok("删除成功", null);
    }

    @PostMapping("/batchRemove")
    @Operation(summary = "批量删除订单", description = "请求体字段：ids 订单主键数组。")
    public ApiResponse<Void> batchRemove(@RequestBody Map<String, List<Long>> body) {
        orders.deleteAllById(body.getOrDefault("ids", List.of()));
        return ApiResponse.ok("删除成功", null);
    }

    @PostMapping("/batchTimeout")
    @Operation(summary = "清理超时订单", description = "将已超过关闭时间且仍未支付的订单清理掉，返回清理数量。")
    public ApiResponse<Integer> batchTimeout() {
        return ApiResponse.ok("清理成功", orderService.cleanTimeoutOrders().size());
    }

    private boolean match(Object actual, String expected) {
        if (expected == null || expected.isBlank()) return true;
        if (actual == null) return false;
        return String.valueOf(actual).contains(expected);
    }
}
