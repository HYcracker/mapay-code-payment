# Mpay Java 版宝塔面板部署教程

本文说明如何把 `mapy_java` 这个 Spring Boot 项目部署到宝塔面板。示例使用正式环境 `prod`，端口 `8080`，数据库 `mpay_prod`。

## 一、服务器环境

建议环境：

- 操作系统：CentOS、Debian、Ubuntu 均可
- 宝塔面板：已安装 Nginx、MySQL
- Java：JDK 17
- Maven：可在本地打包，也可在服务器打包
- 域名：已解析到服务器 IP，例如 `pay.example.com`

宝塔软件商店建议安装：

- Nginx
- MySQL 5.7/8.0
- Java 项目管理器，或使用 Supervisor 管理器
- PM2 管理器不适合 Java 项目，可不装

## 二、上传项目

推荐只上传 Java 项目目录：

```bash
/www/wwwroot/mpay_java
```

目录结构应类似：

```text
/www/wwwroot/mpay_java
├── pom.xml
├── database
│   └── mpay_mysql.sql
├── src
├── public
├── view
└── README.md
```

如果你在本地已经打好 jar，也可以只上传：

```text
/www/wwwroot/mpay_java/app.jar
/www/wwwroot/mpay_java/data
```

但首次部署建议上传完整项目，方便检查配置和数据库文件。

## 三、安装 JDK 17

方式一：宝塔软件商店安装 Java 项目管理器，并在里面安装 JDK 17。

方式二：服务器命令行安装。

Ubuntu/Debian：

```bash
apt update
apt install -y openjdk-17-jdk
java -version
```

CentOS/Rocky：

```bash
yum install -y java-17-openjdk java-17-openjdk-devel
java -version
```

能看到 `17` 版本号即可。

## 四、初始化 MySQL 数据库

项目已经提供数据库初始化脚本：

```bash
cd /www/wwwroot/mpay_java
mysql -uroot -p123456 < database/mpay_mysql.sql
```

该脚本会创建三套数据库和账号：

| 环境 | 数据库 | 账号 | 密码 |
| --- | --- | --- | --- |
| 开发 | `mpay_dev` | `mpay_dev` | `mpay_dev_123456` |
| 测试 | `mpay_test` | `mpay_test` | `mpay_test_123456` |
| 正式 | `mpay_prod` | `mpay_prod` | `mpay_prod_123456` |

宝塔面板如果不允许命令行导入，也可以进入：

```text
数据库 -> phpMyAdmin -> 导入 database/mpay_mysql.sql
```

正式环境默认连接 `mpay_prod`。

## 五、确认正式环境配置

主配置文件：

```text
src/main/resources/application.yml
```

确保启用正式环境：

```yaml
spring:
  profiles:
    active: prod
```

正式环境配置文件：

```text
src/main/resources/application_prod.yml
```

默认内容类似：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/mpay_prod?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: mpay_prod
    password: mpay_prod_123456

server:
  port: 8080

mpay:
  api-docs:
    enabled: false

springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

knife4j:
  enable: false
```

注意：

- 宝塔服务器 MySQL 和应用在同一台机器时，数据库地址用 `127.0.0.1`。
- 如果 MySQL 是远程数据库，请把 `127.0.0.1` 改成远程 MySQL 地址，并放行数据库端口。
- 正式环境默认关闭 Knife4j，避免接口信息暴露。

## 六、打包项目

进入项目目录：

```bash
cd /www/wwwroot/mpay_java
```

执行打包：

```bash
mvn clean package -DskipTests
```

打包成功后会生成：

```text
target/mpay-springboot-2.0.0.jar
```

如果服务器没有 Maven，可以在本地电脑执行同样命令，然后把 jar 上传到服务器。

## 七、启动项目

先用命令行测试启动：

```bash
cd /www/wwwroot/mpay_java
java -jar target/mpay-springboot-2.0.0.jar --spring.profiles.active=prod
```

看到 Spring Boot 启动完成后，在服务器上测试：

```bash
curl http://127.0.0.1:8080/
```

如果能返回首页 HTML，说明应用本身启动成功。

测试完成后按 `Ctrl + C` 停止，接着配置进程守护。

## 八、使用宝塔 Java 项目管理器运行

如果安装了宝塔 Java 项目管理器，推荐这样配置：

- 项目类型：Spring Boot
- 项目名称：`mpay-java`
- 项目路径：`/www/wwwroot/mpay_java`
- Jar 路径：`/www/wwwroot/mpay_java/target/mpay-springboot-2.0.0.jar`
- 项目端口：`8080`
- 启动参数：

```bash
--spring.profiles.active=prod
```

- Java 版本：JDK 17

保存并启动项目。

## 九、使用 Supervisor 运行

如果不用 Java 项目管理器，可以用 Supervisor。

宝塔软件商店安装 `Supervisor 管理器`，新增守护进程：

- 名称：`mpay-java`
- 运行目录：`/www/wwwroot/mpay_java`
- 启动命令：

```bash
java -jar /www/wwwroot/mpay_java/target/mpay-springboot-2.0.0.jar --spring.profiles.active=prod
```

- 运行用户：`www` 或 `root`
- 进程数量：`1`
- 自动启动：开启

保存后启动。

## 十、配置 Nginx 反向代理

在宝塔面板中创建网站：

```text
网站 -> 添加站点 -> pay.example.com
```

站点根目录可以随便选择一个空目录，例如：

```text
/www/wwwroot/pay.example.com
```

然后进入该网站：

```text
设置 -> 配置文件
```

在 `server` 块中加入或替换反向代理配置：

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_http_version 1.1;
    proxy_set_header Connection "";
}
```

保存后重载 Nginx。

访问：

```text
http://pay.example.com/
```

后台登录：

```text
http://pay.example.com/User/login
```

## 十一、配置 HTTPS

宝塔面板进入：

```text
网站 -> pay.example.com -> SSL
```

可以选择：

- Let's Encrypt 免费证书
- 商业证书

开启 SSL 后，建议同时开启：

```text
强制 HTTPS
```

支付回调、异步通知、前端收银台建议使用 HTTPS 域名。

## 十二、配置新订单邮件提醒

项目支持创建新订单后给管理员发送邮件。正式环境配置文件：

```text
/www/wwwroot/mpay_java/src/main/resources/application_prod.yml
```

QQ 邮箱示例：

```yaml
mpay:
  mail:
    enabled: true
    type: qq
    username: your@qq.com
    password: your_mail_auth_code
    from: your@qq.com
    from-name: Mpay
    to:
      - admin@example.com
      - finance@example.com
```

163 邮箱示例：

```yaml
mpay:
  mail:
    enabled: true
    type: 163
    username: your@163.com
    password: your_mail_auth_code
    from: your@163.com
    to:
      - admin@example.com
```

企业邮箱或自建 SMTP 示例：

```yaml
mpay:
  mail:
    enabled: true
    type: custom
    host: smtp.example.com
    port: 465
    username: notice@example.com
    password: your_mail_password_or_auth_code
    from: notice@example.com
    to:
      - admin@example.com
    ssl: true
    starttls: false
```

说明：

- `password` 通常填写 SMTP 授权码，不是网页登录密码。
- `to` 支持配置多个收件邮箱。
- 支持 `qq`、`163`、`126`、`gmail`、`outlook`、`custom`。
- Gmail 和 Outlook 常用 `587 + starttls`，自建邮箱按服务商提供的 SMTP 参数填写。
- 邮件发送失败不会影响订单创建，可以在 Java 项目日志中查看失败原因。

修改配置后需要重新打包并重启 Java 项目。

## 十三、放行端口

宝塔安全组和云服务器安全组至少放行：

- `80`：HTTP
- `443`：HTTPS

`8080` 不建议对公网开放，因为已经通过 Nginx 反向代理访问。只需要本机 `127.0.0.1:8080` 可访问即可。

## 十四、默认登录信息

如果数据库是新初始化，默认后台地址：

```text
https://pay.example.com/User/login
```

默认账号：

```text
admin
```

初始密码：

```text
由 ADMIN_INIT_PASSWORD 环境变量指定；未设置则随机生成并打印到启动日志
```

登录后请立即修改密码，并重新生成商户密钥。

## 十五、接口文档

正式环境 `prod` 默认关闭 Knife4j。

如果临时需要在服务器查看接口文档，可以改：

```text
src/main/resources/application_prod.yml
```

将这些配置改为 `true`：

```yaml
mpay:
  api-docs:
    enabled: true

springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true

knife4j:
  enable: true
```

重启项目后访问：

```text
https://pay.example.com/doc.html
```

查看完成后建议改回关闭，并重启项目。

## 十六、升级部署

升级推荐流程：

1. 备份数据库。
2. 备份当前 jar：

```bash
cp /www/wwwroot/mpay_java/target/mpay-springboot-2.0.0.jar /www/backup/mpay-springboot-2.0.0-$(date +%Y%m%d%H%M%S).jar
```

3. 上传新代码或新 jar。
4. 重新打包：

```bash
cd /www/wwwroot/mpay_java
mvn clean package -DskipTests
```

5. 在宝塔 Java 项目管理器或 Supervisor 中重启项目。
6. 访问首页、后台登录、支付测试、订单列表确认正常。

## 十七、常见问题

### 1. 启动失败，提示找不到 Java

检查 JDK：

```bash
java -version
```

如果没有输出版本号，需要先安装 JDK 17。

### 2. 启动失败，提示数据库连接失败

检查：

- MySQL 是否启动
- `application_prod.yml` 数据库地址、账号、密码是否正确
- 是否已导入 `database/mpay_mysql.sql`
- 宝塔数据库用户是否允许本机连接

可以测试：

```bash
mysql -umpay_prod -pmpay_prod_123456 -h127.0.0.1 mpay_prod
```

### 3. 网站 502 Bad Gateway

说明 Nginx 访问不到 Spring Boot。

检查：

```bash
curl http://127.0.0.1:8080/
```

如果本机访问失败，先去 Java 项目管理器或 Supervisor 查看项目是否启动成功。

### 4. 上传二维码失败

项目默认上传目录：

```text
/www/wwwroot/mpay_java/data/uploads
```

确认运行用户有写入权限：

```bash
mkdir -p /www/wwwroot/mpay_java/data/uploads
chown -R www:www /www/wwwroot/mpay_java/data
chmod -R 755 /www/wwwroot/mpay_java/data
```

如果用 root 运行 Java 项目，一般不会有权限问题，但生产环境更建议使用 `www` 或独立用户运行。

### 5. 登录后样式加载异常

确认 Nginx 反向代理保留了完整路径，并且项目中的 `public`、`view` 目录存在。不要只上传 `src`，否则复刻的前端静态资源会缺失。

### 6. 邮件发送失败

检查：

- 发件邮箱是否开启 SMTP 服务。
- `password` 是否使用授权码。
- 云服务器是否允许访问 SMTP 端口，常见端口为 `465` 或 `587`。
- 企业邮箱的 `host`、`port`、`ssl`、`starttls` 是否与服务商文档一致。
- 宝塔 Java 项目日志中是否有 `新订单邮件提醒发送失败`。

### 7. 支付回调无法收到

检查：

- `notify_url` 必须是公网可访问 HTTPS 地址。
- 业务系统是否放行服务器 IP。
- 域名 SSL 是否正常。
- Nginx 是否正确反代到 `127.0.0.1:8080`。

## 十八、部署完成检查清单

- 首页可访问：`https://pay.example.com/`
- 后台登录可访问：`https://pay.example.com/User/login`
- 能使用管理员账号登录
- 收款账号可以新增、编辑、上传二维码
- 支付测试可以生成正确二维码
- 订单列表可以查询
- Nginx 已开启 HTTPS
- `8080` 未对公网开放
- 正式环境 Knife4j 已关闭
## 补充：新版邮件提醒配置

新版邮件提醒配置已经迁移到数据库表 `mpay_mail_config`，不再通过 `application_prod.yml` 中的 `mpay.mail` 配置。

部署完成并登录后台后，进入：

```text
系统设置 -> 邮件提醒
```

在页面中配置邮箱类型、SMTP 地址、端口、发件账号、授权码、收件邮箱和功能开关。收件邮箱支持多个，可用逗号、分号、空格或换行分隔。

邮件只在新订单创建并提交成功后异步发送。无论邮件未开启、发送成功或发送失败，都不会影响订单创建；发送结果只记录到 `mpay_mail_config` 的 `last_send_status`、`last_send_message`、`last_send_time` 字段以及 Java 应用日志中。
