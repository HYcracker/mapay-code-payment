CREATE DATABASE IF NOT EXISTS `mpay_dev`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `mpay_test`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `mpay_prod`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'mpay_dev'@'%' IDENTIFIED BY 'mpay_dev_123456';
CREATE USER IF NOT EXISTS 'mpay_test'@'%' IDENTIFIED BY 'mpay_test_123456';
CREATE USER IF NOT EXISTS 'mpay_prod'@'%' IDENTIFIED BY 'mpay_prod_123456';
CREATE USER IF NOT EXISTS 'mpay_dev'@'localhost' IDENTIFIED BY 'mpay_dev_123456';
CREATE USER IF NOT EXISTS 'mpay_test'@'localhost' IDENTIFIED BY 'mpay_test_123456';
CREATE USER IF NOT EXISTS 'mpay_prod'@'localhost' IDENTIFIED BY 'mpay_prod_123456';

GRANT ALL PRIVILEGES ON `mpay_dev`.* TO 'mpay_dev'@'%';
GRANT ALL PRIVILEGES ON `mpay_test`.* TO 'mpay_test'@'%';
GRANT ALL PRIVILEGES ON `mpay_prod`.* TO 'mpay_prod'@'%';
GRANT ALL PRIVILEGES ON `mpay_dev`.* TO 'mpay_dev'@'localhost';
GRANT ALL PRIVILEGES ON `mpay_test`.* TO 'mpay_test'@'localhost';
GRANT ALL PRIVILEGES ON `mpay_prod`.* TO 'mpay_prod'@'localhost';

FLUSH PRIVILEGES;

USE `mpay_dev`;

CREATE TABLE IF NOT EXISTS `mpay_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `pid` BIGINT NOT NULL,
  `username` VARCHAR(80) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `secret_key` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(255) DEFAULT NULL,
  `role` VARCHAR(255) DEFAULT 'user',
  `state` INT DEFAULT 1,
  `create_time` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mpay_user_pid` (`pid`),
  UNIQUE KEY `uk_mpay_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `mpay_pay_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `pid` BIGINT DEFAULT NULL,
  `platform` VARCHAR(255) DEFAULT NULL,
  `account` VARCHAR(255) DEFAULT NULL,
  `password` VARCHAR(255) DEFAULT NULL,
  `state` INT DEFAULT 1,
  `pattern` INT DEFAULT 1,
  `params` VARCHAR(255) DEFAULT '{}',
  `remark` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_mpay_pay_account_pid` (`pid`),
  KEY `idx_mpay_pay_account_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `mpay_pay_channel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_id` BIGINT DEFAULT NULL,
  `channel` VARCHAR(255) DEFAULT NULL,
  `type` VARCHAR(255) DEFAULT 'url',
  `qrcode` VARCHAR(1000) DEFAULT NULL,
  `state` INT DEFAULT 1,
  `remark` VARCHAR(255) DEFAULT NULL,
  `daily_limit` DECIMAL(38,2) DEFAULT 0.00,
  `last_time` DATETIME(6) DEFAULT NULL,
  `create_time` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_mpay_pay_channel_account_id` (`account_id`),
  KEY `idx_mpay_pay_channel_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `mpay_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` VARCHAR(255) NOT NULL,
  `pid` BIGINT DEFAULT NULL,
  `type` VARCHAR(255) DEFAULT NULL,
  `out_trade_no` VARCHAR(255) DEFAULT NULL,
  `notify_url` VARCHAR(1000) DEFAULT NULL,
  `return_url` VARCHAR(1000) DEFAULT NULL,
  `name` VARCHAR(255) DEFAULT NULL,
  `money` DECIMAL(38,2) DEFAULT NULL,
  `really_price` DECIMAL(38,2) DEFAULT NULL,
  `clientip` VARCHAR(255) DEFAULT NULL,
  `device` VARCHAR(255) DEFAULT NULL,
  `param` VARCHAR(2000) DEFAULT NULL,
  `state` INT DEFAULT 0,
  `patt` INT DEFAULT 1,
  `platform` VARCHAR(255) DEFAULT NULL,
  `platform_order` VARCHAR(255) NOT NULL DEFAULT '',
  `aid` BIGINT DEFAULT NULL,
  `cid` BIGINT DEFAULT NULL,
  `create_time` DATETIME(6) DEFAULT NULL,
  `close_time` DATETIME(6) DEFAULT NULL,
  `pay_time` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mpay_order_order_id` (`order_id`),
  UNIQUE KEY `uk_mpay_order_out_trade_no` (`out_trade_no`),
  KEY `idx_mpay_order_pid` (`pid`),
  KEY `idx_mpay_order_state_close_time` (`state`, `close_time`),
  KEY `idx_mpay_order_aid_cid_type` (`aid`, `cid`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `mpay_mail_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `enabled` INT DEFAULT 0,
  `type` VARCHAR(50) DEFAULT 'custom',
  `host` VARCHAR(255) DEFAULT '',
  `port` INT DEFAULT 465,
  `username` VARCHAR(255) DEFAULT '',
  `password` VARCHAR(255) DEFAULT '',
  `from_email` VARCHAR(255) DEFAULT '',
  `from_name` VARCHAR(255) DEFAULT 'Mpay',
  `to_emails` VARCHAR(2000) DEFAULT '',
  `auth` INT DEFAULT 1,
  `ssl` INT DEFAULT 1,
  `starttls` INT DEFAULT 0,
  `connection_timeout` INT DEFAULT 10000,
  `timeout` INT DEFAULT 10000,
  `write_timeout` INT DEFAULT 10000,
  `subject_prefix` VARCHAR(255) DEFAULT 'Mpay 新订单提醒',
  `last_send_status` VARCHAR(50) DEFAULT '',
  `last_send_message` VARCHAR(2000) DEFAULT '',
  `last_send_time` DATETIME(6) DEFAULT NULL,
  `create_time` DATETIME(6) DEFAULT NULL,
  `update_time` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `mpay_mail_config` (
  `enabled`, `type`, `host`, `port`, `username`, `password`, `from_email`, `from_name`,
  `to_emails`, `auth`, `ssl`, `starttls`, `connection_timeout`, `timeout`, `write_timeout`,
  `subject_prefix`, `create_time`, `update_time`
)
SELECT
  0, 'custom', '', 465, '', '', '', 'Mpay', '', 1, 1, 0, 10000, 10000, 10000,
  'Mpay 新订单提醒', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM `mpay_mail_config`);

CREATE TABLE IF NOT EXISTS `mpay_test`.`mpay_user` LIKE `mpay_dev`.`mpay_user`;
CREATE TABLE IF NOT EXISTS `mpay_test`.`mpay_pay_account` LIKE `mpay_dev`.`mpay_pay_account`;
CREATE TABLE IF NOT EXISTS `mpay_test`.`mpay_pay_channel` LIKE `mpay_dev`.`mpay_pay_channel`;
CREATE TABLE IF NOT EXISTS `mpay_test`.`mpay_order` LIKE `mpay_dev`.`mpay_order`;
CREATE TABLE IF NOT EXISTS `mpay_test`.`mpay_mail_config` LIKE `mpay_dev`.`mpay_mail_config`;

CREATE TABLE IF NOT EXISTS `mpay_prod`.`mpay_user` LIKE `mpay_dev`.`mpay_user`;
CREATE TABLE IF NOT EXISTS `mpay_prod`.`mpay_pay_account` LIKE `mpay_dev`.`mpay_pay_account`;
CREATE TABLE IF NOT EXISTS `mpay_prod`.`mpay_pay_channel` LIKE `mpay_dev`.`mpay_pay_channel`;
CREATE TABLE IF NOT EXISTS `mpay_prod`.`mpay_order` LIKE `mpay_dev`.`mpay_order`;
CREATE TABLE IF NOT EXISTS `mpay_prod`.`mpay_mail_config` LIKE `mpay_dev`.`mpay_mail_config`;
