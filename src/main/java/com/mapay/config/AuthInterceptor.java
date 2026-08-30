package com.mapay.config;

import com.mapay.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final MapayProperties properties;

    public AuthInterceptor(MapayProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // API 文档路径：如果配置了密码则走 Basic Auth，否则视为公开路径
        if (isDocPath(path)) {
            if (properties.getDocPassword() == null || properties.getDocPassword().isEmpty()) {
                return true;
            }
            if (checkDocAuth(request)) {
                return true;
            }
            response.setHeader("WWW-Authenticate", "Basic realm=\"API Docs\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        if (isPublicPath(path)) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthService.SESSION_USER_ID) != null) {
            return true;
        }
        if (isApiRequest(path, request)) {
            writeUnauthorized(response);
        } else {
            response.sendRedirect(request.getContextPath() + "/User/login");
        }
        return false;
    }

    private boolean isDocPath(String path) {
        return "/doc".equals(path)
                || "/doc.html".equals(path)
                || "/swagger-ui.html".equals(path)
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/webjars/");
    }

    private boolean checkDocAuth(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.regionMatches(true, 0, "Basic ", 0, 6)) {
            return false;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(auth.substring(6).trim()), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon < 0) return false;
            String password = decoded.substring(colon + 1);
            return properties.getDocPassword().equals(password);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPublicPath(String path) {
        return "/".equals(path)
                || "/User/login".equals(path)
                || "/api/user/login".equals(path)
                || "/api/User/login".equals(path)
                || "/favicon.ico".equals(path)
                || "/robots.txt".equals(path)
                || "/error".equals(path)
                || path.startsWith("/component/")
                || path.startsWith("/admin/")
                || path.startsWith("/static/")
                || path.startsWith("/files/")
                || isPaymentPath(path)
                || path.startsWith("/config/")
                || path.startsWith("/confirm/");
    }

    private boolean isPaymentPath(String path) {
        return "/submit".equals(path)
                || "/submit.php".equals(path)
                || "/mapi".equals(path)
                || "/mapi.php".equals(path)
                || path.startsWith("/Pay/console/")
                || path.startsWith("/getOrderState/")
                || path.startsWith("/checkOrder/")
                || "/checkPayResult".equals(path)
                || "/mpayNotify".equals(path)
                || "/payHeart".equals(path)
                || "/validatePayResult".equals(path);
    }

    private boolean isApiRequest(String path, HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        return path.startsWith("/api/")
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":401,\"msg\":\"未登录，请先登录\",\"data\":null}");
    }
}
