package com.mapay.controller;

import com.mapay.model.User;
import com.mapay.repository.PayAccountRepository;
import com.mapay.service.AuthService;
import com.mapay.service.LegacyViewService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PageController {
    private final LegacyViewService views;
    private final AuthService auth;
    private final PayAccountRepository accounts;

    public PageController(LegacyViewService views, AuthService auth, PayAccountRepository accounts) {
        this.views = views;
        this.auth = auth;
        this.accounts = accounts;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String index(HttpServletRequest request) {
        return views.render("index/index", request, Map.of());
    }

    @GetMapping(value = "/doc", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String doc(HttpServletRequest request) {
        return views.render("index/doc", request, Map.of("domain", domain(request)));
    }

    @GetMapping(value = "/User/login", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String login(HttpServletRequest request) {
        return views.render("user/login", request, Map.of());
    }

    @GetMapping(value = "/User/index", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String userIndex(HttpServletRequest request) {
        return views.render("user/index", request, views.userModel(request));
    }

    @GetMapping(value = "/User/setUser", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String setUser(HttpServletRequest request) {
        return views.render("user/set_user", request, views.userModel(request));
    }

    @GetMapping(value = "/Console/index", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String consoleIndex(HttpServletRequest request) {
        return views.render("console/index", request, Map.of("version", "V1 SpringBoot"));
    }

    @GetMapping(value = "/Console/console", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String console(HttpServletRequest request) {
        return views.render("console/console", request, views.dashboardModel());
    }

    @GetMapping(value = "/Order/index", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String orderIndex(HttpServletRequest request) {
        return views.render("order/index", request, Map.of("servertime", now()));
    }

    @GetMapping(value = "/Order/showOrder", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String showOrder(@RequestParam Long id, HttpServletRequest request) {
        return views.render("order/show_order", request, views.orderDetailModel(id));
    }

    @GetMapping(value = "/Order/testPay", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String testPay(HttpServletRequest request) {
        User user = auth.requireUser(request.getSession());
        return views.render("order/test_pay", request, Map.of("pid", user.getPid(), "key", user.getSecretKey()));
    }

    @GetMapping(value = "/PayManage/index", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String payManage(HttpServletRequest request) {
        return views.render("pay_manage/index", request, Map.of("domain", domain(request)));
    }

    @GetMapping(value = "/PayManage/addAccount", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String addAccount(HttpServletRequest request) {
        return views.render("pay_manage/add_account", request, Map.of());
    }

    @GetMapping(value = "/PayManage/editAccount", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String editAccount(@RequestParam Long id, HttpServletRequest request) {
        return views.render("pay_manage/edit_account", request, views.accountModel(id));
    }

    @GetMapping(value = "/PayManage/channelList", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String channelList(@RequestParam Long id, HttpServletRequest request) {
        return views.render("pay_manage/channel_list", request, Map.of("id", id));
    }

    @GetMapping(value = "/PayManage/addChannel", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String addChannel(@RequestParam Long aid, HttpServletRequest request) {
        return accounts.findById(aid)
                .map(account -> views.render(("wxpay".equals(account.getPlatform()) || "alipay".equals(account.getPlatform()))
                        ? "pay_manage/add_channel_code" : "pay_manage/add_channel", request,
                        Map.of("aid", aid, "platform", account.getPlatform() == null ? "" : account.getPlatform(), "account", account.getAccount() == null ? "" : account.getAccount())))
                .orElse("账号不存在");
    }

    @GetMapping(value = "/PayManage/editChannel", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String editChannel(@RequestParam Long cid, HttpServletRequest request) {
        return views.render("pay_manage/edit_channel", request, views.channelModel(cid));
    }

    @GetMapping(value = "/PayManage/payStatistics", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String payStatistics(HttpServletRequest request) {
        return views.render("pay_manage/pay_statistics", request, Map.of());
    }

    @GetMapping(value = "/Plugin/index", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String plugin(HttpServletRequest request) {
        return views.render("plugin/index", request, Map.of());
    }

    @GetMapping(value = "/Settings/mail", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String mailSettings(HttpServletRequest request) {
        return views.render("settings/mail", request, Map.of());
    }

    @GetMapping(value = "/Install/index", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String install(HttpServletRequest request) {
        return views.render("install/index", request, Map.of());
    }

    @GetMapping(value = "/Analytics/index", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String analyticsIndex(HttpServletRequest request) {
        return views.render("analytics/index", request, Map.of());
    }

    private String domain(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }

    private String now() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
