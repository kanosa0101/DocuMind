-- ========================================
-- DocuMind v3.0 数据库表结构
-- 核心设计: 文件即一切，单表模型
-- ========================================

USE `doc_ai`;

-- ========================================
-- file_info: 核心文件表（唯一实体）
-- 合并 file_metadata + doc_info + doc_version 为单表
-- ========================================
CREATE TABLE IF NOT EXISTS `file_info` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',

    -- ===== 基础属性 =====
    `file_uuid`       VARCHAR(36) UNIQUE NOT NULL COMMENT '文件唯一标识（用于物理存储）',
    `file_name`       VARCHAR(255) NOT NULL COMMENT '显示文件名（用户可修改）',
    `original_name`   VARCHAR(255) NOT NULL COMMENT '原始上传文件名',
    `file_type`       VARCHAR(50) DEFAULT NULL COMMENT 'MIME类型',
    `file_size`       BIGINT DEFAULT 0 COMMENT '文件大小(bytes)',
    `storage_path`    VARCHAR(500) NOT NULL COMMENT '当前版本物理存储路径(MinIO)',

    -- ===== AI分析属性 =====
    `summary`         TEXT DEFAULT NULL COMMENT 'AI摘要',
    `keywords`        JSON DEFAULT NULL COMMENT 'AI关键词(JSON数组)',
    `category`        VARCHAR(100) DEFAULT '其他' COMMENT 'AI分类',

    -- ===== 版本属性 =====
    `version`         INT DEFAULT 1 COMMENT '当前版本号',
    `version_history` JSON DEFAULT NULL COMMENT '版本历史(JSON数组)',
    /*
    version_history结构示例:
    [
        {
            "version": 1,
            "file_uuid": "uuid-v1",
            "original_name": "论文A.pdf",
            "storage_path": "minio/path/v1",
            "file_size": 102400,
            "summary": "v1摘要...",
            "keywords": ["AI", "深度学习"],
            "category": "论文",
            "change_summary": "初始版本",
            "create_time": "2026-05-17T10:00:00"
        }
    ]
    */

    -- ===== 知识库属性 =====
    `indexed`         BOOLEAN DEFAULT TRUE COMMENT '是否已索引（默认true）',
    `vector_id`       VARCHAR(100) DEFAULT NULL COMMENT '向量数据库ID',
    `index_time`      DATETIME DEFAULT NULL COMMENT '索引时间',

    -- ===== 处理状态 =====
    `process_status`  VARCHAR(20) DEFAULT 'PENDING' COMMENT '处理状态(PENDING/PROCESSING/COMPLETED/FAILED)',
    `process_time`    DATETIME DEFAULT NULL COMMENT '处理完成时间',
    `retry_count`     INT DEFAULT 0 COMMENT '重试次数',

    -- ===== 用户归属 =====
    `user_id`         BIGINT NOT NULL COMMENT '所属用户ID',
    `status`          VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/DELETED/PROCESSING)',
    `delete_time`     DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- ===== 索引 =====
    INDEX `idx_file_uuid` (`file_uuid`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`),
    INDEX `idx_process_status` (`process_status`),
    INDEX `idx_indexed` (`indexed`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_delete_time` (`delete_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表（v3.0核心表）';

-- ===== 外键约束 =====
ALTER TABLE `file_info`
ADD CONSTRAINT `fk_file_info_user`
FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`)
ON DELETE CASCADE;

-- ========================================
-- file_share: 内部分享表
-- 用户之间分享文件
-- ========================================
CREATE TABLE IF NOT EXISTS `file_share` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `file_uuid`       VARCHAR(36) NOT NULL COMMENT '分享的文件UUID',
    `owner_id`        BIGINT NOT NULL COMMENT '文件所有者ID',
    `share_to_id`     BIGINT NOT NULL COMMENT '分享给的用户ID',
    `permission`      VARCHAR(20) DEFAULT 'VIEW' COMMENT '权限(VIEW/VIEW_DOWNLOAD)',
    `expire_time`     DATETIME DEFAULT NULL COMMENT '过期时间（NULL=永久）',
    `status`          VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/EXPIRED/CANCELLED)',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- ===== 索引 =====
    INDEX `idx_file_uuid` (`file_uuid`),
    INDEX `idx_owner_id` (`owner_id`),
    INDEX `idx_share_to_id` (`share_to_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_expire_time` (`expire_time`),

    -- ===== 唯一约束：同一文件不能重复分享给同一用户 =====
    UNIQUE KEY `uk_file_share` (`file_uuid`, `share_to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分享表';

-- ===== 外键约束 =====
ALTER TABLE `file_share`
ADD CONSTRAINT `fk_file_share_file`
FOREIGN KEY (`file_uuid`) REFERENCES `file_info`(`file_uuid`)
ON DELETE CASCADE;

ALTER TABLE `file_share`
ADD CONSTRAINT `fk_file_share_owner`
FOREIGN KEY (`owner_id`) REFERENCES `sys_user`(`id`)
ON DELETE CASCADE;

ALTER TABLE `file_share`
ADD CONSTRAINT `fk_file_share_to`
FOREIGN KEY (`share_to_id`) REFERENCES `sys_user`(`id`)
ON DELETE CASCADE;

-- ========================================
-- public_share: 公开链接分享表
-- 生成可公开访问的链接
-- ========================================
CREATE TABLE IF NOT EXISTS `public_share` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `share_code`      VARCHAR(32) UNIQUE NOT NULL COMMENT '随机生成的分享码',
    `file_uuid`       VARCHAR(36) NOT NULL COMMENT '分享的文件UUID',
    `owner_id`        BIGINT NOT NULL COMMENT '文件所有者ID',
    `password`        VARCHAR(100) DEFAULT NULL COMMENT '访问密码（可选，加密存储）',
    `download_limit`  INT DEFAULT -1 COMMENT '下载限制（-1=不限）',
    `download_count`  INT DEFAULT 0 COMMENT '已下载次数',
    `expire_time`     DATETIME DEFAULT NULL COMMENT '过期时间',
    `status`          VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/EXPIRED/CANCELLED)',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- ===== 索引 =====
    INDEX `idx_share_code` (`share_code`),
    INDEX `idx_file_uuid` (`file_uuid`),
    INDEX `idx_owner_id` (`owner_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公开链接分享表';

-- ===== 外键约束 =====
ALTER TABLE `public_share`
ADD CONSTRAINT `fk_public_share_file`
FOREIGN KEY (`file_uuid`) REFERENCES `file_info`(`file_uuid`)
ON DELETE CASCADE;

ALTER TABLE `public_share`
ADD CONSTRAINT `fk_public_share_owner`
FOREIGN KEY (`owner_id`) REFERENCES `sys_user`(`id`)
ON DELETE CASCADE;

-- ========================================
-- 保留旧表（兼容过渡期）
-- 以下表在Phase 5清理阶段移除
-- ========================================

-- file_metadata: v2.0文件元数据表（保留）
-- doc_info: v2.0文档表（保留）
-- doc_version: v2.0版本历史表（保留）
-- ai_task: AI任务表（保留）

-- ========================================
-- 回收站表更新（适配v3.0）
-- 注意：MySQL不支持 ADD COLUMN IF NOT EXISTS，手动检查后执行
-- ========================================
-- 如果recycle_bin表缺少file_uuid字段，手动执行以下语句：
-- ALTER TABLE `recycle_bin` ADD COLUMN `file_uuid` VARCHAR(36) DEFAULT NULL COMMENT 'file_info UUID引用' AFTER `file_id`;
-- ALTER TABLE `recycle_bin` ADD INDEX `idx_file_uuid` (`file_uuid`);

-- 检查字段是否存在：
SELECT COLUMN_NAME FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA='doc_ai' AND TABLE_NAME='recycle_bin' AND COLUMN_NAME='file_uuid';

-- ========================================
-- 测试数据验证
-- ========================================

-- 验证表创建
-- SELECT COUNT(*) FROM file_info;
-- SELECT COUNT(*) FROM file_share;
-- SELECT COUNT(*) FROM public_share;

-- 验证索引
-- SHOW INDEX FROM file_info;
-- SHOW INDEX FROM file_share;
-- SHOW INDEX FROM public_share;