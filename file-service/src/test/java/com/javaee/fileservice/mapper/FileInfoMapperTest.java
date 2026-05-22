package com.javaee.fileservice.mapper;

import com.javaee.fileservice.TestConfig;
import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.entity.FileShare;
import com.javaee.fileservice.entity.PublicShare;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileInfoMapper集成测试
 * Phase 1验收标准：所有测试通过
 *
 * 注意：此测试需要数据库连接，使用@Transactional确保测试后数据回滚
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {com.javaee.fileservice.FileServiceApplication.class, TestConfig.class})
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileInfoMapperTest {

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private FileShareMapper fileShareMapper;

    @Autowired
    private PublicShareMapper publicShareMapper;

    private static final Long TEST_USER_ID = 999L;
    private static final String TEST_FILE_UUID = "test-uuid-mapper-001";

    // ===== 基础CRUD测试 =====

    @Test
    @Order(1)
    @DisplayName("插入FileInfo记录")
    void testInsertFileInfo() {
        FileInfo fileInfo = createTestFileInfo(TEST_FILE_UUID, "测试文件.pdf");

        int result = fileInfoMapper.insert(fileInfo);

        assertEquals(1, result);
        assertNotNull(fileInfo.getId());
    }

    @Test
    @Order(2)
    @DisplayName("根据file_uuid查询FileInfo")
    void testSelectByFileUuid() {
        // 先插入
        FileInfo fileInfo = createTestFileInfo("test-uuid-select", "查询测试.pdf");
        fileInfoMapper.insert(fileInfo);

        // 查询
        FileInfo found = fileInfoMapper.selectByFileUuid("test-uuid-select");

        assertNotNull(found);
        assertEquals("查询测试.pdf", found.getFileName());
        assertEquals(TEST_USER_ID, found.getUserId());
    }

    @Test
    @Order(3)
    @DisplayName("根据user_id查询文件列表")
    void testSelectByUserId() {
        // 插入多条测试数据
        fileInfoMapper.insert(createTestFileInfo("test-user-1", "文件1.pdf"));
        fileInfoMapper.insert(createTestFileInfo("test-user-2", "文件2.pdf"));
        fileInfoMapper.insert(createTestFileInfo("test-user-3", "文件3.pdf"));

        // 查询用户文件列表
        List<FileInfo> files = fileInfoMapper.selectByUserId(TEST_USER_ID);

        assertTrue(files.size() >= 3);
    }

    @Test
    @Order(4)
    @DisplayName("根据分类查询文件")
    void testSelectByCategory() {
        FileInfo fileInfo = createTestFileInfo("test-category", "分类测试.pdf");
        fileInfo.setCategory("论文");
        fileInfoMapper.insert(fileInfo);

        List<FileInfo> files = fileInfoMapper.selectByCategory(TEST_USER_ID, "论文");

        assertTrue(files.size() >= 1);
        assertEquals("论文", files.get(0).getCategory());
    }

    @Test
    @Order(5)
    @DisplayName("关键词搜索文件")
    void testSearchByKeyword() {
        FileInfo fileInfo = createTestFileInfo("test-search", "搜索测试.pdf");
        fileInfo.setSummary("这是一个包含关键词的摘要内容");
        fileInfoMapper.insert(fileInfo);

        List<FileInfo> files = fileInfoMapper.searchByKeyword(TEST_USER_ID, "关键词");

        assertTrue(files.size() >= 1);
    }

    // ===== 版本历史测试 =====

    @Test
    @Order(10)
    @DisplayName("更新版本历史JSON")
    void testUpdateVersionHistory() {
        FileInfo fileInfo = createTestFileInfo("test-version", "版本测试.pdf");
        fileInfo.setVersion(1);
        fileInfoMapper.insert(fileInfo);

        String versionHistory = "[{\"version\":1,\"summary\":\"v1摘要\"}]";
        int result = fileInfoMapper.updateVersionHistory("test-version", versionHistory, 2);

        assertEquals(1, result);

        FileInfo updated = fileInfoMapper.selectByFileUuid("test-version");
        assertEquals(2, updated.getVersion());
        assertNotNull(updated.getVersionHistory());
    }

    // ===== AI结果更新测试 =====

    @Test
    @Order(11)
    @DisplayName("更新AI分析结果")
    void testUpdateAiResult() {
        FileInfo fileInfo = createTestFileInfo("test-ai", "AI测试.pdf");
        fileInfo.setProcessStatus("PENDING");
        fileInfoMapper.insert(fileInfo);

        int result = fileInfoMapper.updateAiResult(
                "test-ai",
                "这是AI生成的摘要",
                "[\"AI\",\"测试\"]",
                "技术文档"
        );

        assertEquals(1, result);

        FileInfo updated = fileInfoMapper.selectByFileUuid("test-ai");
        assertEquals("这是AI生成的摘要", updated.getSummary());
        assertEquals("技术文档", updated.getCategory());
        assertEquals("COMPLETED", updated.getProcessStatus());
    }

    // ===== 分类更新测试 =====

    @Test
    @Order(12)
    @DisplayName("更新文件分类")
    void testUpdateCategory() {
        FileInfo fileInfo = createTestFileInfo("test-cat-update", "分类更新.pdf");
        fileInfo.setCategory("其他");
        fileInfoMapper.insert(fileInfo);

        int result = fileInfoMapper.updateCategory("test-cat-update", TEST_USER_ID, "会议记录");

        assertEquals(1, result);

        FileInfo updated = fileInfoMapper.selectByFileUuid("test-cat-update");
        assertEquals("会议记录", updated.getCategory());
    }

    // ===== 软删除测试 =====

    @Test
    @Order(20)
    @DisplayName("软删除文件")
    void testSoftDelete() {
        FileInfo fileInfo = createTestFileInfo("test-delete", "删除测试.pdf");
        fileInfoMapper.insert(fileInfo);

        int result = fileInfoMapper.softDelete("test-delete", TEST_USER_ID);

        assertEquals(1, result);

        FileInfo deleted = fileInfoMapper.selectByFileUuid("test-delete");
        assertEquals("DELETED", deleted.getStatus());
        assertNotNull(deleted.getDeleteTime());
    }

    @Test
    @Order(21)
    @DisplayName("恢复已删除文件")
    void testRestore() {
        FileInfo fileInfo = createTestFileInfo("test-restore", "恢复测试.pdf");
        fileInfo.setStatus("DELETED");
        fileInfo.setDeleteTime(LocalDateTime.now());
        fileInfoMapper.insert(fileInfo);

        int result = fileInfoMapper.restore("test-restore", TEST_USER_ID);

        assertEquals(1, result);

        FileInfo restored = fileInfoMapper.selectByFileUuid("test-restore");
        assertEquals("ACTIVE", restored.getStatus());
        assertNull(restored.getDeleteTime());
    }

    @Test
    @Order(22)
    @DisplayName("查询已删除文件列表")
    void testSelectDeletedByUserId() {
        FileInfo fileInfo = createTestFileInfo("test-deleted-list", "已删除.pdf");
        fileInfo.setStatus("DELETED");
        fileInfo.setDeleteTime(LocalDateTime.now());
        fileInfoMapper.insert(fileInfo);

        List<FileInfo> deletedFiles = fileInfoMapper.selectDeletedByUserId(TEST_USER_ID);

        assertTrue(deletedFiles.size() >= 1);
        assertEquals("DELETED", deletedFiles.get(0).getStatus());
    }

    // ===== 向量索引测试 =====

    @Test
    @Order(30)
    @DisplayName("更新向量索引信息")
    void testUpdateVectorInfo() {
        FileInfo fileInfo = createTestFileInfo("test-vector", "向量测试.pdf");
        fileInfo.setIndexed(false);
        fileInfoMapper.insert(fileInfo);

        int result = fileInfoMapper.updateVectorInfo("test-vector", true, "vec-12345");

        assertEquals(1, result);

        FileInfo updated = fileInfoMapper.selectByFileUuid("test-vector");
        assertTrue(updated.getIndexed());
        assertEquals("vec-12345", updated.getVectorId());
    }

    @Test
    @Order(31)
    @DisplayName("标记向量删除")
    void testMarkVectorDeleted() {
        FileInfo fileInfo = createTestFileInfo("test-vector-del", "向量删除.pdf");
        fileInfo.setIndexed(true);
        fileInfo.setVectorId("vec-to-delete");
        fileInfoMapper.insert(fileInfo);

        int result = fileInfoMapper.markVectorDeleted("test-vector-del");

        assertEquals(1, result);

        FileInfo updated = fileInfoMapper.selectByFileUuid("test-vector-del");
        assertFalse(updated.getIndexed());
    }

    // ===== 处理状态测试 =====

    @Test
    @Order(40)
    @DisplayName("更新处理状态")
    void testUpdateProcessStatus() {
        FileInfo fileInfo = createTestFileInfo("test-process", "状态测试.pdf");
        fileInfo.setProcessStatus("PENDING");
        fileInfoMapper.insert(fileInfo);

        int result = fileInfoMapper.updateProcessStatus("test-process", "PROCESSING");

        assertEquals(1, result);

        FileInfo updated = fileInfoMapper.selectByFileUuid("test-process");
        assertEquals("PROCESSING", updated.getProcessStatus());
    }

    // ===== 统计测试 =====

    @Test
    @Order(50)
    @DisplayName("获取分类统计")
    void testSelectCategoryStats() {
        FileInfo file1 = createTestFileInfo("stat-1", "统计1.pdf");
        file1.setCategory("论文");
        fileInfoMapper.insert(file1);

        FileInfo file2 = createTestFileInfo("stat-2", "统计2.pdf");
        file2.setCategory("论文");
        fileInfoMapper.insert(file2);

        FileInfo file3 = createTestFileInfo("stat-3", "统计3.pdf");
        file3.setCategory("技术文档");
        fileInfoMapper.insert(file3);

        List<java.util.Map<String, Object>> stats = fileInfoMapper.selectCategoryStats(TEST_USER_ID);

        assertTrue(stats.size() >= 2);
    }

    // ===== 分享功能测试 =====

    @Test
    @Order(60)
    @DisplayName("创建内部分享")
    void testCreateFileShare() {
        FileInfo fileInfo = createTestFileInfo("test-share", "分享测试.pdf");
        fileInfoMapper.insert(fileInfo);

        FileShare share = new FileShare();
        share.setFileUuid("test-share");
        share.setOwnerId(TEST_USER_ID);
        share.setShareToId(888L);
        share.setPermission("VIEW");
        share.setStatus("ACTIVE");

        int result = fileShareMapper.insert(share);

        assertEquals(1, result);
    }

    @Test
    @Order(61)
    @DisplayName("创建公开链接分享")
    void testCreatePublicShare() {
        FileInfo fileInfo = createTestFileInfo("test-public", "公开分享.pdf");
        fileInfoMapper.insert(fileInfo);

        PublicShare publicShare = new PublicShare();
        publicShare.setShareCode("share-code-123");
        publicShare.setFileUuid("test-public");
        publicShare.setOwnerId(TEST_USER_ID);
        publicShare.setDownloadLimit(10);
        publicShare.setDownloadCount(0);
        publicShare.setStatus("ACTIVE");

        int result = publicShareMapper.insert(publicShare);

        assertEquals(1, result);
    }

    // ===== 辅助方法 =====

    private FileInfo createTestFileInfo(String fileUuid, String fileName) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileUuid(fileUuid);
        fileInfo.setFileName(fileName);
        fileInfo.setOriginalName(fileName);
        fileInfo.setFileType("application/pdf");
        fileInfo.setFileSize(1024L);
        fileInfo.setStoragePath("documents/" + fileUuid + ".pdf");
        fileInfo.setVersion(1);
        fileInfo.setIndexed(true);
        fileInfo.setProcessStatus("COMPLETED");
        fileInfo.setRetryCount(0);
        fileInfo.setUserId(TEST_USER_ID);
        fileInfo.setStatus("ACTIVE");
        fileInfo.setCreateTime(LocalDateTime.now());
        return fileInfo;
    }
}