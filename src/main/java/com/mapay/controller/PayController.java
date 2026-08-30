package com.mapay.controller;

import com.mapay.common.ApiResponse;
import com.mapay.model.PayChannel;
import com.mapay.model.PaymentOrder;
import com.mapay.repository.PayChannelRepository;
import com.mapay.repository.PaymentOrderRepository;
import com.mapay.service.NotifyService;
import com.mapay.service.OrderService;
import com.mapay.service.LegacyViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "支付接口", description = "易支付兼容下单、收银台、订单状态查询和收款回调")
public class PayController {
    private final OrderService orderService;
    private final PaymentOrderRepository orders;
    private final PayChannelRepository channels;
    private final NotifyService notifyService;
    private final LegacyViewService views;

    public PayController(OrderService orderService, PaymentOrderRepository orders, PayChannelRepository channels, NotifyService notifyService, LegacyViewService views) {
        this.orderService = orderService;
        this.orders = orders;
        this.channels = channels;
        this.notifyService = notifyService;
        this.views = views;
    }

    @RequestMapping({"/submit", "/submit.php"})
    @Operation(summary = "易支付跳转下单", description = "兼容易支付 submit 接口。常用参数：pid 商户 PID；type 支付方式；out_trade_no 商户订单号；notify_url 异步通知地址；return_url 同步跳转地址；name 商品名称；money 订单金额；sign 签名。成功后 302 跳转到收银台。")
    public ResponseEntity<?> submit(@RequestParam Map<String, String> form) {
        PaymentOrder order = orderService.createOrder(form);
        return ResponseEntity.status(302).location(URI.create("/Pay/console/" + order.getOrderId())).build();
    }

    @RequestMapping({"/mapi", "/mapi.php"})
    @Operation(summary = "易支付 API 下单", description = "兼容易支付 mapi 接口。参数同 submit，成功后返回 trade_no 和 payurl。")
    public Map<String, Object> mapi(@RequestParam Map<String, String> form, HttpServletRequest request) {
        PaymentOrder order = orderService.createOrder(form);
        String root = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        return Map.of("code", 1, "msg", "订单创建成功", "trade_no", order.getOrderId(), "payurl", root + "/Pay/console/" + order.getOrderId());
    }

    @GetMapping(value = "/Pay/console/{orderId}", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "打开订单收银台", description = "根据平台订单号渲染支付二维码页面。")
    public String console(@Parameter(description = "平台订单号") @PathVariable String orderId, HttpServletRequest request) {
        PaymentOrder order = orders.findByOrderId(orderId).orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        PayChannel channel = channels.findById(order.getCid()).orElse(new PayChannel());
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("id", order.getId());
        model.put("payUrl", channel.getQrcode());
        model.put("code_type", "image".equals(channel.getType()) || "1".equals(channel.getType()) ? "1" : "0");
        model.put("type", order.getType());
        model.put("order_id", order.getOrderId());
        model.put("out_trade_no", order.getOutTradeNo());
        model.put("name", order.getName());
        model.put("really_price", order.getReallyPrice());
        model.put("patt", order.getPatt());
        model.put("passtime", orderService.passTime(order));
        model.put("payclient", "");
        return views.render("pay/console", request, model);
    }

    @GetMapping("/getOrderState/{orderId}")
    @Operation(summary = "查询订单支付状态", description = "收银台轮询接口。返回订单状态、剩余时间和支付成功后的 return_url。")
    public Map<String, Object> getOrderState(@Parameter(description = "平台订单号") @PathVariable String orderId) {
        PaymentOrder order = orders.findByOrderId(orderId).orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("order_id", order.getOrderId());
        data.put("passtime", orderService.passTime(order));
        data.put("state", order.getState());
        if (order.getState() == 1) {
            data.put("return_url", notifyService.returnUrl(order));
        }
        return data;
    }

    @PostMapping("/validatePayResult")
    @Operation(summary = "更新收银台检测状态", description = "请求体字段：id 订单主键；patt 支付检测状态。订单关闭后不允许更新。")
    public ApiResponse<PaymentOrder> validatePayResult(@RequestBody Map<String, String> body) {
        PaymentOrder order = orders.findById(Long.valueOf(body.get("id"))).orElseThrow();
        if (order.getCloseTime().isBefore(LocalDateTime.now())) {
            return ApiResponse.fail("订单已关闭");
        }
        order.setPatt(Integer.valueOf(body.getOrDefault("patt", String.valueOf(order.getPatt()))));
        return ApiResponse.ok("更新成功", orders.save(order));
    }

    @PostMapping("/payHeart")
    @Operation(summary = "收款监控心跳上报", description = "请求体字段：id 订单主键；platform_order 可选，第三方流水号。用于监控端上报订单已收款。")
    public ApiResponse<PaymentOrder> payHeart(@RequestBody Map<String, String> body) {
        Long id = Long.valueOf(body.get("id"));
        return ApiResponse.ok("收款通知已处理", orderService.markPaid(id, body.getOrDefault("platform_order", "notify")));
    }

    @GetMapping("/checkOrder/{pid}/{sign}")
    @Operation(summary = "监控端检查待支付订单", description = "聚合码/监控端轮询接口，用于判断是否存在待处理订单。")
    public Map<String, Object> checkOrder(@Parameter(description = "商户 PID") @PathVariable Long pid,
                                          @Parameter(description = "请求签名") @PathVariable String sign) {
        int count = orders.findByStateAndCloseTimeAfter(0, LocalDateTime.now()).size();
        return Map.of("code", count > 0 ? 1 : 0, "msg", count > 0 ? "有" + count + "个新订单" : "没有新订单");
    }

    @GetMapping("/checkPayResult")
    @Operation(summary = "检查支付结果兼容接口", description = "保留给第三方监听插件调用的兼容接口。")
    public Map<String, Object> checkPayResult() {
        return Map.of("code", 0, "msg", "Spring Boot 版本已保留接口，第三方监听插件可调用 payHeart 或 mpayNotify 上报结果");
    }

    @PostMapping("/mpayNotify")
    @Operation(summary = "收款插件通知接口", description = "请求体字段：id 订单主键；platform_order 可选，第三方流水号。收到通知后标记订单已支付。")
    public Object mpayNotify(@RequestBody Map<String, String> body) {
        if (!body.containsKey("id")) {
            return "通知数据缺少订单 id";
        }
        orderService.markPaid(Long.valueOf(body.get("id")), body.getOrDefault("platform_order", "mpay"));
        return 200;
    }
}
