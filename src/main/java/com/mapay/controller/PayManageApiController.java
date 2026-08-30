package com.mapay.controller;

import com.mapay.common.ApiResponse;
import com.mapay.common.PageResponse;
import com.mapay.config.MapayProperties;
import com.mapay.model.PayAccount;
import com.mapay.model.PayChannel;
import com.mapay.repository.PayAccountRepository;
import com.mapay.repository.PayChannelRepository;
import com.mapay.repository.PaymentOrderRepository;
import com.mapay.model.PaymentOrder;
import com.mapay.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/payManage", "/api/PayManage"})
@Tag(name = "收款管理", description = "收款账号、收款通道、二维码上传和账号交易统计")
public class PayManageApiController {
    private final PayAccountRepository accounts;
    private final PayChannelRepository channels;
    private final PaymentOrderRepository orders;
    private final AuthService auth;
    private final MapayProperties properties;

    public PayManageApiController(PayAccountRepository accounts, PayChannelRepository channels, PaymentOrderRepository orders, AuthService auth, MapayProperties properties) {
        this.accounts = accounts;
        this.channels = channels;
        this.orders = orders;
        this.auth = auth;
        this.properties = properties;
    }

    @GetMapping("/getPayAccount")
    @Operation(summary = "查询收款账号列表", description = "返回当前登录商户的收款账号及账号收入统计，兼容 Layui 表格格式。")
    public PageResponse<Map<String, Object>> getPayAccount(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                                  @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int limit,
                                                  HttpSession session) {
        Long pid = auth.requirePid(session);
        List<PayAccount> data = accounts.findByPid(pid);
        List<Map<String, Object>> rows = data.stream().skip((long) (page - 1) * limit).limit(limit).map(this::accountRow).toList();
        return PageResponse.of(data.size(), rows);
    }

    @PostMapping("/addAccount")
    @Operation(summary = "新增收款账号", description = "请求体字段：platform 支付平台，如 wxpay/alipay/manual；account 账号名称；password 可选；state 状态；pattern 收款模式；params 扩展参数 JSON。")
    public ApiResponse<PayAccount> addAccount(@RequestBody Map<String, String> body, HttpSession session) {
        PayAccount account = new PayAccount();
        account.setPlatform(body.get("platform"));
        account.setAccount(body.get("account"));
        account.setPassword(body.getOrDefault("password", ""));
        account.setState(parseInt(body.get("state"), 1));
        account.setPattern(parseInt(body.get("pattern"), 1));
        account.setParams(body.getOrDefault("params", "{}"));
        account.setPid(auth.requirePid(session));
        return ApiResponse.ok("添加成功", accounts.save(account));
    }

    @PostMapping("/editAccount")
    @Operation(summary = "编辑收款账号", description = "请求体字段：id 账号主键；platform、account、password、state、pattern、params 按需传入。")
    public ApiResponse<PayAccount> editAccount(@RequestBody Map<String, String> body) {
        PayAccount account = accounts.findById(Long.valueOf(body.get("id"))).orElseThrow();
        account.setPlatform(body.getOrDefault("platform", account.getPlatform()));
        account.setAccount(body.getOrDefault("account", account.getAccount()));
        account.setPassword(body.getOrDefault("password", account.getPassword()));
        account.setState(parseInt(body.get("state"), account.getState()));
        account.setPattern(parseInt(body.get("pattern"), account.getPattern()));
        account.setParams(body.getOrDefault("params", account.getParams()));
        return ApiResponse.ok("修改成功", accounts.save(account));
    }

    @PostMapping("/delAccount")
    @Operation(summary = "删除收款账号", description = "请求体字段：ids 账号主键数组。删除账号时会同步删除该账号下的收款通道。")
    public ApiResponse<Void> delAccount(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.getOrDefault("ids", List.of());
        ids.forEach(id -> channels.deleteAll(channels.findByAccountId(id)));
        accounts.deleteAllById(ids);
        return ApiResponse.ok("已删除", null);
    }

    @PostMapping("/accountEnable")
    @Operation(summary = "启用或禁用收款账号", description = "请求体字段：id 账号主键；state 状态，1 启用，0 禁用。")
    public ApiResponse<PayAccount> accountEnable(@RequestBody Map<String, String> body) {
        PayAccount account = accounts.findById(Long.valueOf(body.get("id"))).orElseThrow();
        account.setState(Integer.valueOf(body.get("state")));
        return ApiResponse.ok("成功", accounts.save(account));
    }

    @PostMapping("/getChannelList")
    @Operation(summary = "查询账号收款通道", description = "请求体字段：aid 收款账号主键。返回该账号下配置的二维码/链接通道。")
    public ApiResponse<List<PayChannel>> getChannelList(@RequestBody Map<String, String> body) {
        return ApiResponse.ok("获取成功", channels.findByAccountId(Long.valueOf(body.get("aid"))));
    }

    @PostMapping("/addChannel")
    @Operation(summary = "新增收款通道", description = "请求体字段：account_id/accountId 收款账号主键；channel 通道标识；type 二维码类型，0 链接，1 图片；qrcode 二维码地址；state 状态。")
    public ApiResponse<PayChannel> addChannel(@RequestBody Map<String, String> body) {
        PayChannel channel = new PayChannel();
        Long accountId = Long.valueOf(body.getOrDefault("account_id", body.getOrDefault("accountId", "0")));
        PayAccount account = accounts.findById(accountId).orElseThrow();
        channel.setAccountId(accountId);
        channel.setChannel(normalizeChannelForAccount(body.get("channel"), account));
        channel.setType(normalizeCodeType(body.getOrDefault("type", "0")));
        channel.setQrcode(body.getOrDefault("qrcode", ""));
        channel.setState(parseInt(body.get("state"), 1));
        channel.setLastTime(LocalDateTime.now());
        return ApiResponse.ok("添加成功", channels.save(channel));
    }

    @PostMapping("/editChannel")
    @Operation(summary = "编辑收款通道", description = "请求体字段：id/cid 通道主键；channel、type、qrcode、state 按需传入。")
    public ApiResponse<PayChannel> editChannel(@RequestBody Map<String, String> body) {
        Long id = Long.valueOf(body.getOrDefault("id", body.getOrDefault("cid", "0")));
        PayChannel channel = channels.findById(id).orElseThrow();
        PayAccount account = accounts.findById(channel.getAccountId()).orElseThrow();
        channel.setChannel(normalizeChannelForAccount(body.getOrDefault("channel", channel.getChannel()), account));
        channel.setType(normalizeCodeType(body.getOrDefault("type", channel.getType())));
        channel.setQrcode(body.getOrDefault("qrcode", channel.getQrcode()));
        channel.setState(parseInt(body.get("state"), channel.getState()));
        return ApiResponse.ok("修改成功", channels.save(channel));
    }

    @PostMapping("/delChannel")
    @Operation(summary = "删除收款通道", description = "请求体字段：id 通道主键。")
    public ApiResponse<Void> delChannel(@RequestBody Map<String, String> body) {
        channels.deleteById(Long.valueOf(body.get("id")));
        return ApiResponse.ok("已删除", null);
    }

    @PostMapping("/uploadQrcode")
    @Operation(summary = "上传收款二维码", description = "表单字段：codeimg 图片文件。上传成功后返回 imgpath，可直接写入收款通道 qrcode。")
    public ApiResponse<Map<String, String>> uploadQrcode(@Parameter(description = "二维码图片文件") @RequestParam("codeimg") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            return ApiResponse.fail("请选择图片文件");
        }
        Path dir = Path.of(properties.getUploadDir(), "qrcode");
        Files.createDirectories(dir);
        String suffix = file.getOriginalFilename() == null ? ".png" : file.getOriginalFilename().replaceAll("^.*(\\.[A-Za-z0-9]+)$", "$1");
        String name = "img_" + System.currentTimeMillis() + suffix;
        file.transferTo(dir.resolve(name));
        return ApiResponse.ok("上传成功", Map.of("imgpath", "/files/qrcode/" + name));
    }

    @GetMapping("/payStatisticsList")
    @Operation(summary = "收款账号统计列表", description = "返回账号维度的收入统计，当前复用收款账号列表数据。")
    public PageResponse<Map<String, Object>> payStatisticsList(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                                      @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int limit,
                                                      HttpSession session) {
        return getPayAccount(page, limit, session);
    }

    @GetMapping("/getAccountTrade")
    @Operation(summary = "查询账号交易明细", description = "按收款账号查询最近交易明细，用于收款统计页展开查看。")
    public ApiResponse<List<Map<String, Object>>> getAccountTrade(@Parameter(description = "收款账号主键") @RequestParam Long aid) {
        return ApiResponse.ok("查询成功", List.of(
                Map.of("order_no", "demo-" + aid, "price", "0.00", "payway", "", "channel", "manual", "time", LocalDateTime.now().toString())
        ));
    }

    private Map<String, Object> accountRow(PayAccount account) {
        int channelNum = channels.findByAccountId(account.getId()).size();
        List<PaymentOrder> paid = orders.findAll().stream()
                .filter(order -> account.getId().equals(order.getAid()) && Integer.valueOf(1).equals(order.getState()))
                .toList();
        BigDecimal total = paid.stream().map(PaymentOrder::getReallyPrice).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.ofEntries(
                Map.entry("id", account.getId()),
                Map.entry("pid", account.getPid()),
                Map.entry("platform", platformName(account.getPlatform())),
                Map.entry("platform_code", account.getPlatform() == null ? "" : account.getPlatform()),
                Map.entry("account", account.getAccount() == null ? "" : account.getAccount()),
                Map.entry("password", account.getPassword() == null ? "" : account.getPassword()),
                Map.entry("state", account.getState() == null ? 0 : account.getState()),
                Map.entry("pattern", account.getPattern() == null ? 0 : account.getPattern()),
                Map.entry("params", account.getParams() == null ? "{}" : account.getParams()),
                Map.entry("channel_num", channelNum),
                Map.entry("income", total),
                Map.entry("day", total),
                Map.entry("yesterday", BigDecimal.ZERO),
                Map.entry("week", total),
                Map.entry("month", total),
                Map.entry("year", total),
                Map.entry("total", total)
        );
    }

    private int parseInt(String value, Integer fallback) {
        if (value == null || value.isBlank()) return fallback == null ? 0 : fallback;
        if ("on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value)) return 1;
        if ("off".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) return 0;
        return Integer.parseInt(value);
    }

    private String platformName(String platform) {
        return switch (platform == null ? "" : platform) {
            case "wxpay" -> "微信支付";
            case "alipay" -> "支付宝";
            case "sqbpay" -> "收钱吧";
            case "manual" -> "通用收款";
            default -> platform == null ? "" : platform;
        };
    }

    private String normalizeCodeType(String type) {
        if ("image".equals(type) || "img".equals(type) || "1".equals(type)) return "1";
        return "0";
    }

    private String normalizeChannelForAccount(String channel, PayAccount account) {
        String platform = account.getPlatform() == null || account.getPlatform().isBlank() ? "manual" : account.getPlatform();
        String accountName = account.getAccount() == null || account.getAccount().isBlank() ? String.valueOf(account.getId()) : account.getAccount();
        String prefix = switch (platform) {
            case "wxpay" -> "wxpay1";
            case "alipay" -> "alipay1";
            default -> platform;
        };
        if (channel != null && !channel.isBlank()) {
            if ("wxpay".equals(platform) && channel.matches("(?i)^wxpay\\d*#.*")) return channel;
            if ("alipay".equals(platform) && channel.matches("(?i)^alipay\\d*#.*")) return channel;
            if (!"wxpay".equals(platform) && !"alipay".equals(platform)) return channel;
        }
        return prefix + "#" + accountName;
    }
}
