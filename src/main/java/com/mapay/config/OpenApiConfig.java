package com.mapay.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mpay.api-docs", name = "enabled", havingValue = "true")
public class OpenApiConfig {
    private static final String CONTROLLER_PACKAGE = "com.mapay.controller";

    @Bean
    public OpenAPI mpayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mpay Java API")
                        .description("码支付 Java Spring Boot 版本接口文档")
                        .version("2.0.0")
                        .contact(new Contact().name("Mpay").url("https://gitee.com/HYcracker/mapay-code-payment"))
                        .license(new License().name("MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("业务开发文档")
                        .url("/doc"));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("全部接口")
                .packagesToScan(CONTROLLER_PACKAGE)
                .pathsToMatch(
                        "/api/**",
                        "/Console/menu",
                        "/Console/message",
                        "/install/**",
                        "/Install/**",
                        "/submit",
                        "/submit.php",
                        "/mapi",
                        "/mapi.php",
                        "/Pay/**",
                        "/getOrderState/**",
                        "/checkOrder/**",
                        "/checkPayResult",
                        "/mpayNotify",
                        "/payHeart",
                        "/validatePayResult"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("后台管理接口")
                .packagesToScan(CONTROLLER_PACKAGE)
                .pathsToMatch("/api/**", "/Console/menu", "/Console/message", "/install/**", "/Install/**")
                .build();
    }

    @Bean
    public GroupedOpenApi payApi() {
        return GroupedOpenApi.builder()
                .group("支付兼容接口")
                .packagesToScan(CONTROLLER_PACKAGE)
                .pathsToMatch("/submit", "/submit.php", "/mapi", "/mapi.php", "/Pay/**", "/getOrderState/**", "/checkOrder/**", "/checkPayResult", "/mpayNotify", "/payHeart", "/validatePayResult")
                .build();
    }
}
