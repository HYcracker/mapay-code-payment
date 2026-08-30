# Mpay Java Spring Boot 版本

这是从原 ThinkPHP 项目中分离出来的 Java Spring Boot 独立项目。请在 `mapy_java` 目录内运行命令；仓库根目录仍保留原 PHP 源码。

## 环境与数据库

项目通过 `src/main/resources/application.yml` 中的 `spring.profiles.active` 切换环境。公共配置保留在 `application.yml`，不同环境的数据库、端口和 Knife4j 开关分别写在独立配置文件中：

- `src/main/resources/application_dev.yml`
- `src/main/resources/application_test.yml`
- `src/main/resources/application_prod.yml`

初始化数据库：

```bash
cd mapy_java
mysql -uroot -p123456 < database/mpay_mysql.sql
```

应用启动后，Hibernate 会根据实体继续自动补齐字段差异。

## 启动

需要 JDK 17 和 Maven。

```bash
cd mapy_java
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

正式环境：

```bash
cd mapy_java
mvn clean package
java -jar target/mpay-springboot-2.0.0.jar --spring.profiles.active=prod
```

## 访问地址

- 项目首页：`http://localhost:8080/`
- 后台登录：`http://localhost:8080/User/login`
- 后台主页：`http://localhost:8080/Console/index`
- 开发文档页：`http://localhost:8080/doc`
- 默认账号：`admin`
- 默认密码：`admin123`

## 接口文档

开发环境和测试环境默认开启 Knife4j/OpenAPI：

- Knife4j：`http://localhost:8080/doc.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- Swagger UI：`http://localhost:8080/swagger-ui.html`

正式环境默认关闭接口文档，避免接口结构暴露。

## 新订单邮件提醒

邮件提醒配置已写入数据库表 `mpay_mail_config`，不再通过 `application.yml` 配置。管理员登录后台后进入：

```text
系统设置 -> 邮件提醒
```

可配置项包括：

- 是否开启新订单邮件提醒
- 邮箱类型：`qq`、`163`、`126`、`gmail`、`outlook`、`custom`
- SMTP 地址、端口、发件账号、授权码、发件邮箱、发件名称
- 收件邮箱，多个邮箱可用逗号、分号、空格或换行分隔
- SMTP 认证、SSL、STARTTLS、连接/读取/写入超时
- 邮件标题前缀

新订单创建成功并提交事务后，系统会异步发送邮件。邮件内容会携带平台订单号、商户订单号、PID、支付方式、商品名称、订单金额、实付金额、订单状态、创建时间、关闭时间、通知地址和跳转地址。

无论邮件功能是否开启、发送成功或失败，都不会影响订单创建。发送结果只记录到 `mpay_mail_config.last_send_status`、`last_send_message`、`last_send_time` 和 Java 应用日志中。

## 已实现功能

- 商户登录、退出、资料修改、密码重置
- 未登录访问保护：未登录只能访问首页、登录页、首页开发文档页和支付发起/查询等公开接口
- 收款账号管理、收款通道管理、二维码上传
- 易支付兼容下单：`/submit`、`/mapi`
- 收银台：`/Pay/console/{orderId}`
- 订单状态查询：`/getOrderState/{orderId}`
- 订单列表、补单、删除、重新通知、清理超时订单
- 首页统计接口：`/api/console/orderinfo`
- 插件中心列表
- 系统设置：新订单邮件提醒配置
- 复用原项目 Layui + PearAdmin 后台 UI、首页、开发文档、订单页、收款管理页、插件页、用户中心、安装页和收银台

## 主要 UI 路由

- `/User/login` 登录页
- `/Console/index` PearAdmin 后台框架
- `/Console/console` 仪表盘
- `/PayManage/index` 收款账号管理
- `/PayManage/channelList?id=1` 收款通道列表
- `/PayManage/payStatistics` 收款统计
- `/Order/index` 订单管理
- `/Order/testPay` 支付测试
- `/Plugin/index` 插件中心
- `/Settings/mail` 邮件提醒配置
- `/User/index` 用户中心
- `/doc` 首页开发文档
- `/Install/index` 安装页

## 签名规则

参数按键名升序排列，排除 `sign`、`sign_type` 和空值，拼接为 `k=v&...` 后追加商户密钥，再取 MD5。
