package com.mapay.service;

import com.mapay.common.SignUtils;
import com.mapay.config.MapayProperties;
import com.mapay.model.PayAccount;
import com.mapay.model.PayChannel;
import com.mapay.model.PaymentOrder;
import com.mapay.model.User;
import com.mapay.repository.PayAccountRepository;
import com.mapay.repository.PayChannelRepository;
import com.mapay.repository.PaymentOrderRepository;
import com.mapay.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class OrderService {
    private final UserRepository users;
    private final PayAccountRepository accounts;
    private final PayChannelRepository channels;
    private final PaymentOrderRepository orders;
    private final NotifyService notifyService;
    private final OrderMailService orderMailService;
    private final MapayProperties properties;
    private final Random random = new Random();

    public OrderService(UserRepository users, PayAccountRepository accounts, PayChannelRepository channels,
                        PaymentOrderRepository orders, NotifyService notifyService, OrderMailService orderMailService, MapayProperties properties) {
        this.users = users;
        this.accounts = accounts;
        this.channels = channels;
        this.orders = orders;
        this.notifyService = notifyService;
        this.orderMailService = orderMailService;
        this.properties = properties;
    }

    public User verifyMerchant(Map<String, String> req) {
        requireParams(req, "pid", "sign");
        Long pid = parseLong(req.get("pid"), "pid");
        User user = users.findByPidAndState(pid, 1).orElseThrow(() -> new IllegalArgumentException("用户禁用或不存在"));
        String sign = SignUtils.sign(req, user.getSecretKey());
        if (!sign.equalsIgnoreCase(req.get("sign"))) {
            throw new IllegalArgumentException("签名错误");
        }
        return user;
    }

    @Transactional
    public PaymentOrder createOrder(Map<String, String> req) {
        requireParams(req, "pid", "type", "out_trade_no", "notify_url", "name", "money", "sign");
        verifyMerchant(req);
        if (orders.existsByOutTradeNo(req.get("out_trade_no"))) {
            throw new IllegalArgumentException("订单提交重复");
        }
        SelectedChannel selected = selectChannel(parseLong(req.get("pid"), "pid"), req.get("type"));
        BigDecimal money = parseMoney(req.get("money"));
        BigDecimal reallyPrice = checkMoney(money, req.get("type"), selected.account().getId(), selected.channel().getId(), selected.channel().getChannel());

        PaymentOrder order = new PaymentOrder();
        order.setOrderId(createUniqueOrderId());
        order.setPid(parseLong(req.get("pid"), "pid"));
        order.setType(req.get("type"));
        order.setOutTradeNo(req.get("out_trade_no"));
        order.setNotifyUrl(req.get("notify_url"));
        order.setReturnUrl(req.getOrDefault("return_url", ""));
        order.setName(req.get("name"));
        order.setMoney(money);
        order.setReallyPrice(reallyPrice);
        order.setClientip(req.getOrDefault("clientip", ""));
        order.setDevice(req.getOrDefault("device", ""));
        order.setParam(req.getOrDefault("param", ""));
        order.setState(0);
        order.setPatt(selected.account().getPattern());
        order.setPlatform(resolveOrderPlatform(req.get("type"), selected.account().getPlatform()));
        order.setPlatformOrder("");
        order.setAid(selected.account().getId());
        order.setCid(selected.channel().getId());
        order.setCreateTime(LocalDateTime.now());
        order.setCloseTime(LocalDateTime.now().plusSeconds(properties.getOrderExpireSeconds()));
        order.setPayTime(null);
        String rawToken = java.util.UUID.randomUUID().toString().replace("-", "");
        order.setConfirmToken(SignUtils.sha256(rawToken));
        selected.channel().setLastTime(LocalDateTime.now());
        channels.save(selected.channel());
        PaymentOrder saved = orders.save(order);
        notifyNewOrderAfterCommit(saved, rawToken);
        return saved;
    }

    @Transactional
    public PaymentOrder markPaid(Long id, String platformOrder) {
        PaymentOrder order = orders.findById(id).orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        order.setState(1);
        order.setPayTime(LocalDateTime.now());
        order.setPlatformOrder(platformOrder == null ? "" : platformOrder);
        PaymentOrder saved = orders.save(order);
        notifyService.send(saved);
        return saved;
    }

    @Transactional
    public PaymentOrder markPaidWithConfirm(Long id, String platformOrder, String confirmIp) {
        PaymentOrder order = orders.findById(id).orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        order.setState(1);
        order.setPayTime(LocalDateTime.now());
        order.setPlatformOrder(platformOrder == null ? "" : platformOrder);
        order.setConfirmIp(confirmIp == null ? "" : confirmIp);
        order.setConfirmTime(LocalDateTime.now());
        PaymentOrder saved = orders.save(order);
        notifyService.send(saved);
        return saved;
    }

    @Transactional
    public List<PaymentOrder> cleanTimeoutOrders() {
        List<PaymentOrder> timeout = orders.findByStateAndCloseTimeBefore(0, LocalDateTime.now());
        orders.deleteAll(timeout);
        return timeout;
    }

    public long passTime(PaymentOrder order) {
        long seconds = java.time.Duration.between(LocalDateTime.now(), order.getCloseTime()).toSeconds();
        return Math.max(seconds, 0);
    }

    private SelectedChannel selectChannel(Long pid, String type) {
        List<PayAccount> activeAccounts = accounts.findByPidAndState(pid, 1);
        List<Long> aids = activeAccounts.stream().map(PayAccount::getId).toList();
        if (aids.isEmpty()) {
            throw new IllegalArgumentException("用户无可用收款账户");
        }
        List<PayChannel> available = channels.findByAccountIdInAndStateOrderByLastTimeAsc(aids, 1);
        PayChannel channel = available.stream()
                .filter(item -> accountMatchesType(item.getAccountId(), activeAccounts, type))
                .filter(item -> matchesType(item.getChannel(), type))
                .findFirst()
                .orElseGet(() -> selectFallbackChannel(available, activeAccounts, type));
        if (channel == null) {
            throw new IllegalArgumentException("用户账户无可用收款通道，请检查是否已启用 " + type + " 收款账号及对应通道");
        }
        PayAccount account = accounts.findById(channel.getAccountId()).orElseThrow(() -> new IllegalArgumentException("收款通道未绑定有效账号"));
        return new SelectedChannel(account, channel);
    }

    private PayChannel selectFallbackChannel(List<PayChannel> available, List<PayAccount> activeAccounts, String type) {
        if ("wxpay".equals(type) || "alipay".equals(type)) {
            return available.stream()
                    .filter(item -> accountMatchesType(item.getAccountId(), activeAccounts, type))
                    .filter(item -> item.getChannel() == null || item.getChannel().isBlank() || matchesType(item.getChannel(), type))
                    .findFirst()
                    .orElse(null);
        }
        return available.stream()
                .filter(item -> item.getChannel() == null || (!item.getChannel().startsWith("wxpay") && !item.getChannel().startsWith("alipay")))
                .findFirst()
                .orElse(null);
    }

    private String resolveOrderPlatform(String type, String accountPlatform) {
        if ("wxpay".equals(type) || "alipay".equals(type)) {
            return type;
        }
        return accountPlatform;
    }

    private boolean accountMatchesType(Long accountId, List<PayAccount> activeAccounts, String type) {
        return activeAccounts.stream()
                .filter(account -> account.getId() != null && account.getId().equals(accountId))
                .anyMatch(account -> type != null && type.equals(account.getPlatform()));
    }

    private boolean matchesType(String channel, String type) {
        if (channel == null || type == null) {
            return false;
        }
        return ("wxpay".equals(type) && channel.matches("(?i)^wxpay\\d*#.*"))
                || ("alipay".equals(type) && channel.matches("(?i)^alipay\\d*#.*"))
                || (!"wxpay".equals(type) && !"alipay".equals(type));
    }

    private BigDecimal checkMoney(BigDecimal money, String type, Long aid, Long cid, String channelCode) {
        if (channelCode != null && channelCode.matches("(?i)^alipay4#\\d+$")) {
            return money;
        }
        BigDecimal result = money;
        List<BigDecimal> active = orders.findByAidAndCidAndTypeAndStateAndCloseTimeAfter(aid, cid, type, 0, LocalDateTime.now())
                .stream().map(PaymentOrder::getReallyPrice).toList();
        while (active.contains(result)) {
            result = result.add(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        }
        return result;
    }

    private String createOrderId() {
        return "H" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + (1000 + random.nextInt(9000));
    }

    private String createUniqueOrderId() {
        for (int i = 0; i < 20; i++) {
            String orderId = createOrderId();
            if (orders.findByOrderId(orderId).isEmpty()) {
                return orderId;
            }
        }
        return "H" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + (1000 + random.nextInt(9000));
    }

    private void notifyNewOrderAfterCommit(PaymentOrder order, String rawToken) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    orderMailService.notifyNewOrder(order, rawToken);
                }
            });
        } else {
            orderMailService.notifyNewOrder(order, rawToken);
        }
    }

    private void requireParams(Map<String, String> req, String... names) {
        for (String name : names) {
            String value = req.get(name);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("缺少必要参数：" + name);
            }
        }
    }

    private Long parseLong(String value, String name) {
        try {
            return Long.valueOf(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("参数格式错误：" + name);
        }
    }

    private BigDecimal parseMoney(String value) {
        try {
            BigDecimal money = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
            if (money.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("订单金额必须大于 0");
            }
            return money;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("参数格式错误：money");
        }
    }

    private record SelectedChannel(PayAccount account, PayChannel channel) {
    }
}
