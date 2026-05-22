package com.javaee.fileservice.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileInfo实体类单元测试
 * Phase 1验收标准：所有测试通过
 */
class FileInfoTest {

    // ===== 实体创建测试 =====

    @Test
    @DisplayName("创建FileInfo实体应成功")
    void testCreateFileInfo() {
        FileInfo fileInfo = new FileInfo();

        fileInfo.setFileUuid("test-uuid-123");
        fileInfo.setFileName("论文A.pdf");
        fileInfo.setOriginalName("论文A.pdf");
        fileInfo.setFileType("application/pdf");
        fileInfo.setFileSize(102400L);
        fileInfo.setStoragePath("documents/test-uuid-123.pdf");
        fileInfo.setSummary("这是AI摘要");
        fileInfo.setKeywords("[\"AI\",\"深度学习\"]");
        fileInfo.setCategory("论文");
        fileInfo.setVersion(1);
        fileInfo.setIndexed(true);
        fileInfo.setUserId(1L);
        fileInfo.setStatus("ACTIVE");
        fileInfo.setCreateTime(LocalDateTime.now());

        assertEquals("test-uuid-123", fileInfo.getFileUuid());
        assertEquals("论文A.pdf", fileInfo.getFileName());
        assertEquals("application/pdf", fileInfo.getFileType());
        assertEquals(102400L, fileInfo.getFileSize());
        assertEquals("论文", fileInfo.getCategory());
        assertEquals(1, fileInfo.getVersion());
        assertTrue(fileInfo.getIndexed());
        assertEquals("ACTIVE", fileInfo.getStatus());
    }

    @Test
    @DisplayName("默认值验证")
    void testDefaultValues() {
        FileInfo fileInfo = new FileInfo();

        assertNull(fileInfo.getFileUuid());
        assertNull(fileInfo.getFileName());
        assertNull(fileInfo.getVersion());  // 需要设置默认值1
        assertNull(fileInfo.getIndexed());  // 需要设置默认值true
        assertNull(fileInfo.getStatus());   // 需要设置默认值ACTIVE
    }

    // ===== 版本历史字段测试 =====

    @Test
    @DisplayName("设置version_history JSON字符串")
    void testSetVersionHistory() {
        FileInfo fileInfo = new FileInfo();
        String versionHistory = "[{\"version\":1,\"file_uuid\":\"v1-uuid\",\"summary\":\"v1摘要\"}]";

        fileInfo.setVersionHistory(versionHistory);

        assertEquals(versionHistory, fileInfo.getVersionHistory());
    }

    @Test
    @DisplayName("version_history可以是空JSON数组")
    void testEmptyVersionHistory() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setVersionHistory("[]");

        assertEquals("[]", fileInfo.getVersionHistory());
    }

    @Test
    @DisplayName("version_history可以为null表示无历史")
    void testNullVersionHistory() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setVersionHistory(null);

        assertNull(fileInfo.getVersionHistory());
    }

    // ===== 关键词字段测试 =====

    @Test
    @DisplayName("keywords存储为JSON字符串")
    void testKeywordsJsonStorage() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setKeywords("[\"AI\",\"深度学习\",\"神经网络\"]");

        assertNotNull(fileInfo.getKeywords());
        assertTrue(fileInfo.getKeywords().contains("AI"));
        assertTrue(fileInfo.getKeywords().contains("深度学习"));
    }

    @Test
    @DisplayName("keywords可以为null")
    void testNullKeywords() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setKeywords(null);

        assertNull(fileInfo.getKeywords());
    }

    // ===== 处理状态测试 =====

    @Test
    @DisplayName("processStatus字段测试")
    void testProcessStatus() {
        FileInfo fileInfo = new FileInfo();

        // 测试各种状态
        fileInfo.setProcessStatus("PENDING");
        assertEquals("PENDING", fileInfo.getProcessStatus());

        fileInfo.setProcessStatus("PROCESSING");
        assertEquals("PROCESSING", fileInfo.getProcessStatus());

        fileInfo.setProcessStatus("COMPLETED");
        assertEquals("COMPLETED", fileInfo.getProcessStatus());

        fileInfo.setProcessStatus("FAILED");
        assertEquals("FAILED", fileInfo.getProcessStatus());
    }

    // ===== 软删除状态测试 =====

    @Test
    @DisplayName("status字段支持ACTIVE和DELETED")
    void testStatusField() {
        FileInfo fileInfo = new FileInfo();

        fileInfo.setStatus("ACTIVE");
        assertEquals("ACTIVE", fileInfo.getStatus());

        fileInfo.setStatus("DELETED");
        assertEquals("DELETED", fileInfo.getStatus());

        fileInfo.setDeleteTime(LocalDateTime.now());
        assertNotNull(fileInfo.getDeleteTime());
    }

    @Test
    @DisplayName("软删除应设置delete_time")
    void testSoftDelete() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setStatus("ACTIVE");
        fileInfo.setDeleteTime(null);

        // 模拟软删除
        fileInfo.setStatus("DELETED");
        fileInfo.setDeleteTime(LocalDateTime.now());

        assertEquals("DELETED", fileInfo.getStatus());
        assertNotNull(fileInfo.getDeleteTime());
    }

    @Test
    @DisplayName("恢复应清除delete_time")
    void testRestore() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setStatus("DELETED");
        fileInfo.setDeleteTime(LocalDateTime.now());

        // 模拟恢复
        fileInfo.setStatus("ACTIVE");
        fileInfo.setDeleteTime(null);

        assertEquals("ACTIVE", fileInfo.getStatus());
        assertNull(fileInfo.getDeleteTime());
    }

    // ===== 知识库属性测试 =====

    @Test
    @DisplayName("indexed字段测试")
    void testIndexedField() {
        FileInfo fileInfo = new FileInfo();

        fileInfo.setIndexed(true);
        assertTrue(fileInfo.getIndexed());

        fileInfo.setIndexed(false);
        assertFalse(fileInfo.getIndexed());
    }

    @Test
    @DisplayName("vectorId字段测试")
    void testVectorIdField() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setVectorId("vec-12345");
        fileInfo.setIndexTime(LocalDateTime.now());

        assertEquals("vec-12345", fileInfo.getVectorId());
        assertNotNull(fileInfo.getIndexTime());
    }

    // ===== 用户归属测试 =====

    @Test
    @DisplayName("userId字段验证")
    void testUserIdField() {
        FileInfo fileInfo = new FileInfo();

        fileInfo.setUserId(1L);
        assertEquals(1L, fileInfo.getUserId());

        fileInfo.setUserId(5L);
        assertEquals(5L, fileInfo.getUserId());
    }

    // ===== 时间字段测试 =====

    @Test
    @DisplayName("时间字段可以正确设置和获取")
    void testTimeFields() {
        FileInfo fileInfo = new FileInfo();
        LocalDateTime now = LocalDateTime.now();

        fileInfo.setCreateTime(now);
        fileInfo.setUpdateTime(now);
        fileInfo.setProcessTime(now);
        fileInfo.setIndexTime(now);
        fileInfo.setDeleteTime(now);

        assertEquals(now, fileInfo.getCreateTime());
        assertEquals(now, fileInfo.getUpdateTime());
        assertEquals(now, fileInfo.getProcessTime());
        assertEquals(now, fileInfo.getIndexTime());
        assertEquals(now, fileInfo.getDeleteTime());
    }

    // ===== 重试次数测试 =====

    @Test
    @DisplayName("retryCount字段测试")
    void testRetryCount() {
        FileInfo fileInfo = new FileInfo();

        fileInfo.setRetryCount(0);
        assertEquals(0, fileInfo.getRetryCount());

        fileInfo.setRetryCount(3);
        assertEquals(3, fileInfo.getRetryCount());
    }

    // ===== 完整实体验证 =====

    @Test
    @DisplayName("创建完整FileInfo实体并验证所有字段")
    void testCompleteFileInfo() {
        FileInfo fileInfo = new FileInfo();

        // 设置所有字段
        fileInfo.setFileUuid("complete-test-uuid");
        fileInfo.setFileName("完整测试.pdf");
        fileInfo.setOriginalName("完整测试_原始.pdf");
        fileInfo.setFileType("application/pdf");
        fileInfo.setFileSize(204800L);
        fileInfo.setStoragePath("documents/complete-test-uuid.pdf");
        fileInfo.setSummary("这是完整的AI摘要内容");
        fileInfo.setKeywords("[\"测试\",\"完整\",\"验证\"]");
        fileInfo.setCategory("技术文档");
        fileInfo.setVersion(2);
        fileInfo.setVersionHistory("[{\"version\":1,\"summary\":\"v1摘要\"}]");
        fileInfo.setIndexed(true);
        fileInfo.setVectorId("vec-complete-123");
        fileInfo.setProcessStatus("COMPLETED");
        fileInfo.setRetryCount(0);
        fileInfo.setUserId(5L);
        fileInfo.setStatus("ACTIVE");

        LocalDateTime now = LocalDateTime.now();
        fileInfo.setCreateTime(now.minusDays(1));
        fileInfo.setUpdateTime(now);
        fileInfo.setProcessTime(now);
        fileInfo.setIndexTime(now);

        // 验证所有字段
        assertEquals("complete-test-uuid", fileInfo.getFileUuid());
        assertEquals("完整测试.pdf", fileInfo.getFileName());
        assertEquals("完整测试_原始.pdf", fileInfo.getOriginalName());
        assertEquals("application/pdf", fileInfo.getFileType());
        assertEquals(204800L, fileInfo.getFileSize());
        assertEquals("documents/complete-test-uuid.pdf", fileInfo.getStoragePath());
        assertEquals("这是完整的AI摘要内容", fileInfo.getSummary());
        assertNotNull(fileInfo.getKeywords());
        assertEquals("技术文档", fileInfo.getCategory());
        assertEquals(2, fileInfo.getVersion());
        assertNotNull(fileInfo.getVersionHistory());
        assertTrue(fileInfo.getIndexed());
        assertEquals("vec-complete-123", fileInfo.getVectorId());
        assertEquals("COMPLETED", fileInfo.getProcessStatus());
        assertEquals(0, fileInfo.getRetryCount());
        assertEquals(5L, fileInfo.getUserId());
        assertEquals("ACTIVE", fileInfo.getStatus());
        assertNotNull(fileInfo.getCreateTime());
        assertNotNull(fileInfo.getUpdateTime());
        assertNotNull(fileInfo.getProcessTime());
        assertNotNull(fileInfo.getIndexTime());
    }
}