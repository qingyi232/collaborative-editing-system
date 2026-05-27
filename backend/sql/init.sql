-- 在线协作编辑系统 数据库初始化脚本
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS collab_edit DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE collab_edit;

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `email` VARCHAR(100) COMMENT '邮箱',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: USER/DOC_ADMIN/SYS_ADMIN',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username(`username`),
    INDEX idx_role(`role`)
) ENGINE=InnoDB COMMENT='用户表';

-- 文档表
CREATE TABLE IF NOT EXISTS `document` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL COMMENT '文档标题',
    `content` LONGTEXT COMMENT '文档内容',
    `owner_id` BIGINT NOT NULL COMMENT '创建者ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-已删除 1-正常 2-归档',
    `is_public` TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开: 0-私有 1-公开',
    `current_version` INT NOT NULL DEFAULT 1 COMMENT '当前版本号',
    `doc_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文档大小(字节)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner(`owner_id`),
    INDEX idx_status(`status`),
    FOREIGN KEY (`owner_id`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB COMMENT='文档表';

-- 文档成员表（权限管理）
CREATE TABLE IF NOT EXISTS `document_member` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `permission` VARCHAR(20) NOT NULL DEFAULT 'VIEW' COMMENT '权限: VIEW/EDIT/ADMIN',
    `invited_by` BIGINT COMMENT '邀请人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doc_user(`document_id`, `user_id`),
    INDEX idx_user(`user_id`),
    FOREIGN KEY (`document_id`) REFERENCES `document`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB COMMENT='文档成员表';

-- 文档版本表（快照存储）
CREATE TABLE IF NOT EXISTS `document_version` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `version_number` INT NOT NULL COMMENT '版本号',
    `content` LONGTEXT COMMENT '快照内容',
    `content_hash` VARCHAR(64) COMMENT '内容哈希',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `change_summary` VARCHAR(500) COMMENT '变更摘要',
    `doc_size` BIGINT NOT NULL DEFAULT 0 COMMENT '版本大小(字节)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doc_version(`document_id`, `version_number`),
    FOREIGN KEY (`document_id`) REFERENCES `document`(`id`),
    FOREIGN KEY (`operator_id`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB COMMENT='文档版本表';

-- 操作日志表（OT操作记录）
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `user_id` BIGINT NOT NULL COMMENT '操作用户ID',
    `operation_type` VARCHAR(30) NOT NULL COMMENT '操作类型: INSERT/DELETE/RETAIN/FORMAT',
    `operation_data` TEXT COMMENT '操作数据(JSON)',
    `base_version` INT NOT NULL COMMENT '基于版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doc_version(`document_id`, `base_version`),
    INDEX idx_user(`user_id`),
    FOREIGN KEY (`document_id`) REFERENCES `document`(`id`)
) ENGINE=InnoDB COMMENT='操作日志表';

-- 评论表
CREATE TABLE IF NOT EXISTS `document_comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID(回复)',
    `position_info` VARCHAR(200) COMMENT '评论关联位置(JSON)',
    `resolved` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已解决: 0-未解决 1-已解决',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_doc(`document_id`),
    INDEX idx_user(`user_id`),
    FOREIGN KEY (`document_id`) REFERENCES `document`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB COMMENT='文档评论表';

-- 系统审计日志
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT COMMENT '操作用户ID',
    `username` VARCHAR(50) COMMENT '操作用户名',
    `action` VARCHAR(50) NOT NULL COMMENT '操作动作',
    `target_type` VARCHAR(30) COMMENT '目标类型: DOCUMENT/USER/SYSTEM',
    `target_id` BIGINT COMMENT '目标ID',
    `detail` TEXT COMMENT '操作详情(JSON)',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user(`user_id`),
    INDEX idx_action(`action`),
    INDEX idx_time(`created_at`)
) ENGINE=InnoDB COMMENT='系统审计日志表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    `config_value` VARCHAR(500) COMMENT '配置值',
    `description` VARCHAR(200) COMMENT '配置说明',
    `updated_by` BIGINT COMMENT '修改人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_key(`config_key`)
) ENGINE=InnoDB COMMENT='系统配置表';

-- 插入默认系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('max_storage_per_user', '104857600', '每用户最大存储空间(字节) 默认100MB'),
('version_retention_days', '90', '版本保留天数 默认90天'),
('max_doc_size', '10485760', '单文档最大大小(字节) 默认10MB'),
('max_collaborators', '20', '单文档最大协作人数'),
('websocket_heartbeat_interval', '30', 'WebSocket心跳间隔(秒)');

-- 插入默认管理员账户 (密码: admin123)
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '$2a$10$hfNeTGBkfEINldI.Q5ydY.TNM/4zcsNjowivh6HQCyOVk2T/diE/O', '系统管理员', 'SYS_ADMIN');

-- 插入文档管理员账户 (密码: docadmin123)
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role`) VALUES
('docadmin', '$2a$10$hfNeTGBkfEINldI.Q5ydY.TNM/4zcsNjowivh6HQCyOVk2T/diE/O', '文档管理员', 'DOC_ADMIN');
