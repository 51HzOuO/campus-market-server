-- 新增业务模块所需表（MySQL 8+）。在部署数据库中执行一次。
-- 不包含账号、密码或任何环境变量值。

CREATE TABLE IF NOT EXISTS `errand_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `publisher_id` BIGINT NOT NULL,
  `runner_id` BIGINT NULL,
  `title` VARCHAR(120) NOT NULL,
  `description` VARCHAR(1000) NOT NULL,
  `pickup_location` VARCHAR(255) NOT NULL,
  `delivery_location` VARCHAR(255) NOT NULL,
  `distance_km` DECIMAL(8,2) NOT NULL DEFAULT 0,
  `weight_kg` DECIMAL(8,2) NOT NULL DEFAULT 0,
  `urgent` TINYINT NOT NULL DEFAULT 0,
  `contact_phone` VARCHAR(32) NULL,
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `payment_status` TINYINT NOT NULL DEFAULT 0,
  `audit_status` TINYINT NOT NULL DEFAULT 0,
  `audit_remark` VARCHAR(255) NULL,
  `accepted_time` DATETIME NULL,
  `paid_time` DATETIME NULL,
  `completed_time` DATETIME NULL,
  `cancelled_time` DATETIME NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), INDEX `idx_errand_audit_status` (`audit_status`), INDEX `idx_errand_publisher` (`publisher_id`), INDEX `idx_errand_runner` (`runner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `second_hand_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(120) NOT NULL,
  `description` VARCHAR(2000) NOT NULL,
  `images` JSON NULL,
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `category` VARCHAR(50) NULL,
  `location` VARCHAR(255) NULL,
  `contact` VARCHAR(100) NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `audit_status` TINYINT NOT NULL DEFAULT 0,
  `audit_remark` VARCHAR(255) NULL,
  `buyer_id` BIGINT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), INDEX `idx_item_audit_status` (`audit_status`), INDEX `idx_item_status` (`status`), INDEX `idx_item_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `club` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `creator_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(2000) NOT NULL,
  `logo` VARCHAR(500) NULL,
  `contact` VARCHAR(100) NULL,
  `member_count` INT NOT NULL DEFAULT 1,
  `status` TINYINT NOT NULL DEFAULT 0,
  `reject_reason` VARCHAR(255) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), INDEX `idx_club_status` (`status`), INDEX `idx_club_creator` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `club_activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `club_id` BIGINT NOT NULL,
  `creator_id` BIGINT NOT NULL,
  `title` VARCHAR(120) NOT NULL,
  `content` VARCHAR(3000) NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NULL,
  `location` VARCHAR(255) NOT NULL,
  `max_participants` INT NULL,
  `participant_count` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0,
  `reject_reason` VARCHAR(255) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), INDEX `idx_activity_status` (`status`), INDEX `idx_activity_club` (`club_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `club_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `club_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `role` TINYINT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_club_user` (`club_id`, `user_id`), INDEX `idx_member_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `club_activity_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `activity_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`), INDEX `idx_activity_member_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
