-- 创建文档表
CREATE TABLE IF NOT EXISTS `document` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '文档ID',
  `title` VARCHAR(255) NOT NULL COMMENT '文档标题',
  `content` TEXT COMMENT '文档内容',
  `summary` TEXT COMMENT '文档摘要',
  `keywords` TEXT COMMENT '关键词（JSON格式）',
  `file_id` VARCHAR(64) COMMENT '关联文件ID',
  `user_id` BIGINT COMMENT '创建用户ID',
  `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-活跃，deleted-已删除',
  `version` INT DEFAULT 1 COMMENT '版本号',
  `category` VARCHAR(50) COMMENT '分类',
  `tags` TEXT COMMENT '标签（JSON格式）',
  `created_by` VARCHAR(64) COMMENT '创建人',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- 创建文档版本表
CREATE TABLE IF NOT EXISTS `document_version` (
  `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '版本ID',
  `document_id` VARCHAR(64) NOT NULL COMMENT '所属文档ID',
  `version_number` INT NOT NULL COMMENT '版本号',
  `title` VARCHAR(255) NOT NULL COMMENT '文档标题',
  `content` TEXT COMMENT '文档内容',
  `summary` TEXT COMMENT '文档摘要',
  `keywords` TEXT COMMENT '关键词',
  `change_log` VARCHAR(500) COMMENT '变更日志',
  `created_by` VARCHAR(64) COMMENT '创建人',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_document_id` (`document_id`),
  INDEX `idx_version_number` (`version_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档版本表';
