package com.mapay.config;

import com.mapay.model.PayAccount;
import com.mapay.model.PayChannel;
import com.mapay.model.User;
import com.mapay.repository.PayAccountRepository;
import com.mapay.repository.PayChannelRepository;
import com.mapay.repository.UserRepository;
import com.mapay.common.SignUtils;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableAsync
@EnableConfigurationProperties(MapayProperties.class)
public class AppConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;

    public AppConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CommandLineRunner seed(UserRepository users, PayAccountRepository accounts, PayChannelRepository channels,
                           PasswordEncoder encoder) {
        return args -> {
            if (users.count() == 0) {
                User admin = new User();
                admin.setPid(1000L);
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("admin123"));
                admin.setNickname("管理员");
                admin.setSecretKey(randomKey());
                admin.setRole("admin");
                admin.setState(1);
                users.save(admin);

                PayAccount account = new PayAccount();
                account.setPid(admin.getPid());
                account.setPlatform("manual");
                account.setAccount("demo-account");
                account.setPassword("");
                account.setState(1);
                account.setPattern(1);
                account.setParams("{}");
                accounts.save(account);

                PayChannel channel = new PayChannel();
                channel.setAccountId(account.getId());
                channel.setChannel("manual#1000");
                channel.setType("url");
                channel.setQrcode("https://example.com/pay");
                channel.setState(1);
                channel.setRemark("演示收款通道");
                channel.setDailyLimit(BigDecimal.ZERO);
                channels.save(channel);
            }
            channels.findAll().forEach(channel -> accounts.findById(channel.getAccountId()).ifPresent(account -> {
                String platform = account.getPlatform();
                String code = channel.getChannel();
                if ("wxpay".equals(platform) && (code == null || !code.matches("(?i)^wxpay\\d*#.*"))) {
                    channel.setChannel("wxpay1#" + fallbackAccountName(account));
                    channels.save(channel);
                } else if ("alipay".equals(platform) && (code == null || !code.matches("(?i)^alipay\\d*#.*"))) {
                    channel.setChannel("alipay1#" + fallbackAccountName(account));
                    channels.save(channel);
                }
            }));
        };
    }

    private static String fallbackAccountName(PayAccount account) {
        return account.getAccount() == null || account.getAccount().isBlank() ? String.valueOf(account.getId()) : account.getAccount();
    }

    private static String randomKey() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return SignUtils.md5(HexFormat.of().formatHex(bytes));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**").addResourceLocations("file:./data/uploads/");
        registry.addResourceHandler("/component/**").addResourceLocations("file:./public/component/");
        registry.addResourceHandler("/admin/**").addResourceLocations("file:./public/admin/");
        registry.addResourceHandler("/static/**").addResourceLocations("file:./public/static/");
        registry.addResourceHandler("/config/**").addResourceLocations("file:./public/config/");
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");
    }
}
