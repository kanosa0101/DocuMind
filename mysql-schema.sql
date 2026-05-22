-- ========================================
-- Nacos 配置数据库表结构
-- 来源: https://github.com/alibaba/nacos/blob/develop/distribution/conf/mysql-schema.sql
-- ========================================

-- 创建 Nacos 数据库
CREATE DATABASE IF NOT EXISTS `nacos_config` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `nacos_config`;

-- 配置信息表
CREATE TABLE `config_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id` varchar(255) NOT NULL COMMENT 'data_id',
    `group_id` varchar(128) DEFAULT NULL COMMENT 'group_id',
    `content` longtext NOT NULL COMMENT 'content',
    `md5` varchar(32) DEFAULT NULL COMMENT 'md5',
    `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `src_user` text COMMENT 'source user',
    `src_ip` varchar(50) DEFAULT NULL COMMENT 'source ip',
    `app_name` varchar(128) DEFAULT NULL,
    `tenant_id` varchar(128) DEFAULT '' COMMENT '租户字段',
    `c_desc` varchar(256) DEFAULT NULL,
    `c_use` varchar(64) DEFAULT NULL,
    `effect` varchar(64) DEFAULT NULL,
    `type` varchar(64) DEFAULT NULL,
    `c_schema` text,
    `encrypted_data_key` text NOT NULL COMMENT '密钥',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfo_datagrouptenant` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='config_info';

-- 配置信息聚合表
CREATE TABLE `config_info_aggr` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id` varchar(255) NOT NULL COMMENT 'data_id',
    `group_id` varchar(128) DEFAULT NULL COMMENT 'group_id',
    `datum_id` varchar(255) NOT NULL COMMENT 'datum_id',
    `content` longtext NOT NULL COMMENT '内容',
    `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `src_user` text COMMENT 'source user',
    `src_ip` varchar(50) DEFAULT NULL COMMENT 'source ip',
    `tenant_id` varchar(128) DEFAULT '' COMMENT '租户字段',
    `app_name` varchar(128) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfoaggr_datagrouptenantdatum` (`data_id`,`group_id`,`tenant_id`,`datum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='增加租户字段';

-- 配置信息标签表
CREATE TABLE `config_info_tag` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id` varchar(255) NOT NULL COMMENT 'data_id',
    `group_id` varchar(128) DEFAULT NULL COMMENT 'group_id',
    `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
    `tag_id` varchar(128) NOT NULL COMMENT 'tag_id',
    `app_name` varchar(128) DEFAULT NULL,
    `content` longtext NOT NULL COMMENT 'content',
    `md5` varchar(32) DEFAULT NULL COMMENT 'md5',
    `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `src_user` text COMMENT 'source user',
    `src_ip` varchar(50) DEFAULT NULL COMMENT 'source ip',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfotag_datagrouptenanttag` (`data_id`,`group_id`,`tenant_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='config_info_tag';

-- 配置历史表
CREATE TABLE `config_info_history` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `data_id` varchar(255) NOT NULL,
    `group_id` varchar(128) DEFAULT NULL,
    `content` longtext NOT NULL,
    `md5` varchar(32) DEFAULT NULL,
    `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP,
    `src_user` text,
    `src_ip` varchar(50) DEFAULT NULL,
    `op_type` char(10) DEFAULT NULL,
    `tenant_id` varchar(128) DEFAULT '',
    `encrypted_data_key` text NOT NULL COMMENT '密钥',
    PRIMARY KEY (`id`),
    KEY `idx_configinfo_history_datagrouptenant` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='config_info_history';

-- 配置发布表
CREATE TABLE `config_info_relation` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id` varchar(255) NOT NULL COMMENT 'data_id',
    `group_id` varchar(128) DEFAULT NULL COMMENT 'group_id',
    `app_name` varchar(128) DEFAULT NULL,
    `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `src_user` text COMMENT 'source user',
    `src_ip` varchar(50) DEFAULT NULL COMMENT 'source ip',
    `tenant_id` varchar(128) DEFAULT '' COMMENT '租户字段',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinforelation_datagrouptenantapp` (`data_id`,`group_id`,`tenant_id`,`app_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='config_info_relation';

-- 集群表
CREATE TABLE `cluster_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `cluster_name` varchar(128) NOT NULL COMMENT '集群名称',
    `cluster_addr` varchar(256) NOT NULL COMMENT '集群地址',
    `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cluster_name` (`cluster_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='cluster_info';

-- 用户表
CREATE TABLE `users` (
    `username` varchar(50) NOT NULL PRIMARY KEY,
    `password` varchar(500) NOT NULL,
    `enabled` boolean NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- 角色表
CREATE TABLE `roles` (
    `username` varchar(50) NOT NULL,
    `role` varchar(50) NOT NULL,
    UNIQUE INDEX `idx_user_role` (`username` ASC, `role` ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- 权限表
CREATE TABLE `permissions` (
    `role` varchar(50) NOT NULL,
    `resource` varchar(255) NOT NULL,
    `action` varchar(8) NOT NULL,
    UNIQUE INDEX `uk_role_permission` (`role`,`resource`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='权限信息表';

-- 插入默认用户 (nacos/nacos)
INSERT INTO `users` (`username`, `password`, `enabled`) VALUES ('nacos', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', TRUE);
INSERT INTO `roles` (`username`, `role`) VALUES ('nacos', 'ROLE_ADMIN');

-- ========================================
-- DocuMind 业务数据库表结构
-- ========================================

CREATE DATABASE IF NOT EXISTS `doc_ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `doc_ai`;

-- 用户表
CREATE TABLE `sys_user` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `password` varchar(100) NOT NULL COMMENT '密码',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
    `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
    `status` tinyint(1) DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    `role` varchar(20) DEFAULT 'user' COMMENT '角色 admin/user',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 文件元数据表 (file-service 使用，替代原 file_info 表)
-- 原 file_info 表已废弃，使用 file_metadata 替代
CREATE TABLE `file_metadata` (
    `id` varchar(36) NOT NULL COMMENT '主键ID',
    `file_id` varchar(36) NOT NULL COMMENT '文件唯一标识',
    `file_name` varchar(255) DEFAULT NULL COMMENT '存储文件名',
    `original_file_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
    `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
    `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型(MIME)',
    `file_size` bigint(20) DEFAULT 0 COMMENT '文件大小(字节)',
    `md5` varchar(32) DEFAULT NULL COMMENT '文件MD5校验值',
    `storage_type` varchar(20) DEFAULT 'minio' COMMENT '存储类型 local/minio',
    `bucket_name` varchar(100) DEFAULT NULL COMMENT 'MinIO存储桶名称',
    `object_key` varchar(255) DEFAULT NULL COMMENT 'MinIO对象键',
    `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/DELETED',
    `create_by` varchar(50) DEFAULT NULL COMMENT '创建者',
    `user_id` bigint(20) DEFAULT NULL COMMENT '上传用户ID（用户隔离）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_file_id` (`file_id`),
    KEY `idx_file_name` (`file_name`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据表';

-- 文档表
CREATE TABLE `doc_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `title` varchar(255) NOT NULL COMMENT '文档标题',
    `content` longtext COMMENT '文档内容',
    `summary` text COMMENT 'AI摘要',
    `keywords` varchar(500) DEFAULT NULL COMMENT '关键词',
    `category` varchar(100) DEFAULT NULL COMMENT '分类',
    `tags` varchar(500) DEFAULT NULL COMMENT '标签',
    `file_id` varchar(36) DEFAULT NULL COMMENT '关联文件ID（UUID）',
    `user_id` bigint(20) NOT NULL COMMENT '创建用户ID',
    `version` int(11) DEFAULT 1 COMMENT '版本号',
    `status` tinyint(1) DEFAULT 1 COMMENT '状态 0-删除 1-正常',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

-- 文档版本历史表
CREATE TABLE `doc_version` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '版本ID',
    `doc_id` bigint(20) NOT NULL COMMENT '文档ID',
    `version` int(11) NOT NULL COMMENT '版本号',
    `content` longtext COMMENT '版本内容',
    `change_summary` varchar(500) DEFAULT NULL COMMENT '变更摘要',
    `user_id` bigint(20) NOT NULL COMMENT '操作用户ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档版本历史表';

-- AI 任务表
CREATE TABLE `ai_task` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `task_type` varchar(50) NOT NULL COMMENT '任务类型 summary/keyword/correct/chat',
    `input_data` text COMMENT '输入数据',
    `output_data` text COMMENT '输出结果',
    `doc_id` bigint(20) DEFAULT NULL COMMENT '关联文档ID',
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `status` varchar(20) DEFAULT 'pending' COMMENT '状态 pending/processing/completed/failed',
    `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI任务表';

-- 插入默认管理员用户
-- 密码: test123 (BCrypt标准哈希，已验证可登录)
-- 哈希: $2a$10$rvHLvSplN0WqyeWDHspMbup5LszFnbWOEys..8z0rcb9JZORb4o26 (60字符)
-- 生产环境请务必修改默认密码！
INSERT INTO `sys_user` (`username`, `password`, `email`, `nickname`, `role`, `status`)
VALUES ('admin', '$2a$10$rvHLvSplN0WqyeWDHspMbup5LszFnbWOEys..8z0rcb9JZORb4o26', 'admin@documind.com', '系统管理员', 'admin', 1);

-- 插入默认普通用户
-- 密码: test123 (BCrypt标准哈希，已验证可登录)
-- 生产环境请务必修改默认密码！
INSERT INTO `sys_user` (`username`, `password`, `email`, `nickname`, `role`, `status`)
VALUES ('user', '$2a$10$rvHLvSplN0WqyeWDHspMbup5LszFnbWOEys..8z0rcb9JZORb4o26', 'user@documind.com', '普通用户', 'user', 1);

-- ========================================
-- 回收站表 (文件软删除后存储)
-- ========================================
CREATE TABLE IF NOT EXISTS `recycle_bin` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `recycle_id` varchar(36) NOT NULL COMMENT '回收站记录唯一标识(UUID)',
    `file_id` varchar(36) DEFAULT NULL COMMENT '关联的文件ID',
    `bucket_name` varchar(100) DEFAULT NULL COMMENT 'MinIO存储桶名称',
    `object_name` varchar(255) NOT NULL COMMENT '原始对象名称',
    `original_file_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
    `file_size` bigint(20) DEFAULT 0 COMMENT '文件大小(字节)',
    `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型(MIME)',
    `user_id` bigint(20) NOT NULL COMMENT '删除用户ID',
    `delete_time` datetime NOT NULL COMMENT '删除时间',
    `expiry_time` datetime NOT NULL COMMENT '过期时间(自动永久删除)',
    `status` varchar(20) DEFAULT 'DELETED' COMMENT '状态 DELETED/RESTORED/PERMANENT_DELETED',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recycle_id` (`recycle_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_file_id` (`file_id`),
    KEY `idx_expiry_time` (`expiry_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回收站表';

-- ========================================
-- 外键约束 (确保数据一致性)
-- ========================================

-- 文档表外键
-- 注意: 添加外键前需确保数据一致，否则可能导致约束失败
-- 如需在新环境部署，建议在数据初始化后添加

-- doc_info.user_id → sys_user.id (用户删除时级联删除文档)
ALTER TABLE `doc_info`
ADD CONSTRAINT `fk_doc_user`
FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`)
ON DELETE CASCADE;

-- doc_info.file_id → file_metadata.file_id (文件删除时允许文档保留)
ALTER TABLE `doc_info`
ADD CONSTRAINT `fk_doc_file`
FOREIGN KEY (`file_id`) REFERENCES `file_metadata`(`file_id`)
ON DELETE SET NULL;

-- doc_version.doc_id → doc_info.id (文档删除时级联删除版本)
ALTER TABLE `doc_version`
ADD CONSTRAINT `fk_version_doc`
FOREIGN KEY (`doc_id`) REFERENCES `doc_info`(`id`)
ON DELETE CASCADE;

-- doc_version 唯一约束 (同一文档的同一版本号唯一)
ALTER TABLE `doc_version`
ADD UNIQUE KEY `uk_doc_version` (`doc_id`, `version`);

-- 回收站外键
-- recycle_bin.file_id → file_metadata.file_id (文件恢复时关联)
ALTER TABLE `recycle_bin`
ADD CONSTRAINT `fk_recycle_file`
FOREIGN KEY (`file_id`) REFERENCES `file_metadata`(`file_id`)
ON DELETE SET NULL;