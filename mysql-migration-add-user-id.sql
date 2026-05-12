-- ========================================
-- 文件元数据表添加用户隔离字段
-- 修复安全漏洞：用户文件未隔离，所有用户可以看到所有文件
-- ========================================

USE `doc_ai`;

-- 1. 添加 user_id 字段到 file_metadata 表
ALTER TABLE `file_metadata`
ADD COLUMN `user_id` bigint(20) DEFAULT NULL COMMENT '上传用户ID' AFTER `create_by`;

-- 2. 添加 user_id 索引以提高查询性能
ALTER TABLE `file_metadata`
ADD INDEX `idx_user_id` (`user_id`);

-- 3. 为已有数据设置默认用户ID（如果有数据的话）
-- 这里假设已有数据属于管理员用户ID=1
-- UPDATE `file_metadata` SET `user_id` = 1 WHERE `user_id` IS NULL;