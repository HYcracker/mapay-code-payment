package com.mapay.controller;

import com.mapay.common.SignUtils;
import com.mapay.model.PaymentOrder;
import com.mapay.repository.PaymentOrderRepository;
import com.mapay.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ConfirmController {
    private final PaymentOrderRepository orders;
    private final OrderService orderService;

    public ConfirmController(PaymentOrderRepository orders, OrderService orderService) {
        this.orders = orders;
        this.orderService = orderService;
    }

    @GetMapping("/confirm/{token}")
    public String confirmPage(@PathVariable String token, Model model) {
        PaymentOrder order = orders.findByConfirmToken(SignUtils.sha256(token)).orElse(null);
        if (order == null) {
            model.addAttribute("error", "无效的确认链接");
            return "confirm";
        }
        model.addAttribute("order", order);
        model.addAttribute("token", token);
        model.addAttribute("expired", order.getCloseTime() != null && order.getCloseTime().isBefore(LocalDateTime.now()));
        return "confirm";
    }

    @PostMapping("/confirm/{token}/approve")
    public String approve(@PathVariable String token,
                          @RequestParam(defaultValue = "admin_confirm") String platformOrder,
                          RedirectAttributes redirectAttributes,
                          HttpServletRequest request) {
        String hashed = SignUtils.sha256(token);
        PaymentOrder order = orders.findByConfirmToken(hashed).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "无效的确认链接");
            return "redirect:/confirm/" + token;
        }
        if (order.getState() != null && order.getState() == 1) {
            redirectAttributes.addFlashAttribute("error", "该订单已支付，无需重复确认");
            return "redirect:/confirm/" + token;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        orderService.markPaidWithConfirm(order.getId(), platformOrder, ip);
        redirectAttributes.addFlashAttribute("success", "订单已确认支付，系统已回调通知");
        return "redirect:/confirm/" + token;
    }
}
