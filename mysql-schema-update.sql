-- DocuMind 数据库迁移脚本
-- 版本: V004__add_v2.0_version_fields.sql
-- 描述: 添加文件-文档版本关联相关字段（v2.0）

-- ========== V003: 文档处理状态字段 ==========

-- 添加处理状态字段
ALTER TABLE doc_info ADD COLUMN process_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '处理状态: PENDING/PARSE/AI_PROCESSING/INDEXING/COMPLETED/FAILED/NEED_UPDATE';

-- 添加重试次数字段
ALTER TABLE doc_info ADD COLUMN retry_count INT DEFAULT 0 COMMENT 'AI处理重试次数';

-- 添加处理完成时间字段
ALTER TABLE doc_info ADD COLUMN process_time DATETIME COMMENT 'AI处理完成时间';

-- 更新现有记录的处理状态为已完成（历史数据兼容）
UPDATE doc_info SET process_status = 'COMPLETED', process_time = update_time WHERE summary IS NOT NULL;
UPDATE doc_info SET process_status = 'PENDING' WHERE summary IS NULL OR summary = '';

-- ========== V004: v2.0文件-文档版本关联字段 ==========

-- FileMetadata表新增字段（文件中心版本管理）
ALTER TABLE file_metadata
ADD COLUMN version_of_doc VARCHAR(50) COMMENT '所属文档ID（可选，关联到文档版本）',
ADD COLUMN version_num INT DEFAULT 1 COMMENT '版本号',
ADD COLUMN previous_file_id VARCHAR(50) COMMENT '上一个版本的文件ID（可选，建立版本链）';

-- FileMetadata表新增索引
ALTER TABLE file_metadata
ADD INDEX idx_version_of_doc (version_of_doc),
ADD INDEX idx_previous_file_id (previous_file_id);

-- DocumentVersion表新增字段（版本历史完整保留AI结果）
ALTER TABLE doc_version
ADD COLUMN file_id VARCHAR(50) COMMENT '该版本关联的文件ID（跳转文件中心）',
ADD COLUMN summary TEXT COMMENT '该版本的AI摘要（历史保留）',
ADD COLUMN keywords VARCHAR(500) COMMENT '该版本的AI关键词（历史保留）';

-- DocumentVersion表新增索引
ALTER TABLE doc_version
ADD INDEX idx_file_id (file_id);

-- 更新现有数据：将Document的fileId同步到最新版本的DocumentVersion
-- 注意：此操作需要根据实际数据情况执行，这里仅提供参考
-- UPDATE doc_version dv JOIN doc_info d ON dv.doc_id = d.id
-- SET dv.file_id = d.file_id WHERE dv.version = d.version;