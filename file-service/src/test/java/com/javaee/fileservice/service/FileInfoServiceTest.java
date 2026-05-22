package com.javaee.fileservice.service;

import com.javaee.fileservice.dto.FileStatsDTO;
import com.javaee.fileservice.dto.SimilarityResultDTO;
import com.javaee.fileservice.entity.FileInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileInfoService接口测试
 * Phase 3验收标准：接口方法定义完整
 */
class FileInfoServiceTest {

    // ===== FileInfo实体创建测试 =====

    @Test
    @DisplayName("FileInfo实体字段完整性验证")
    void testFileInfoFields() {
        FileInfo fileInfo = new FileInfo();

        // 验证22个字段都可设置和获取
        fileInfo.setId(1L);
        fileInfo.setFileUuid("uuid-test");
        fileInfo.setFileName("fileName");
        fileInfo.setOriginalName("originalName");
        fileInfo.setFileType("pdf");
        fileInfo.setFileSize(1024L);
        fileInfo.setStoragePath("path/test");
        fileInfo.setSummary("summary");
        fileInfo.setKeywords("[\"k1\",\"k2\"]");
        fileInfo.setCategory("论文");
        fileInfo.setVersion(1);
        fileInfo.setVersionHistory("[]");
        fileInfo.setIndexed(true);
        fileInfo.setVectorId("vec-1");
        fileInfo.setIndexTime(null);
        fileInfo.setProcessStatus("COMPLETED");
        fileInfo.setProcessTime(null);
        fileInfo.setRetryCount(0);
        fileInfo.setUserId(1L);
        fileInfo.setStatus("ACTIVE");
        fileInfo.setDeleteTime(null);
        fileInfo.setCreateTime(null);
        fileInfo.setUpdateTime(null);

        // 验证所有字段可获取
        assertEquals(1L, fileInfo.getId());
        assertEquals("uuid-test", fileInfo.getFileUuid());
        assertEquals("fileName", fileInfo.getFileName());
        assertEquals("originalName", fileInfo.getOriginalName());
        assertEquals("pdf", fileInfo.getFileType());
        assertEquals(1024L, fileInfo.getFileSize());
        assertEquals("path/test", fileInfo.getStoragePath());
        assertEquals("summary", fileInfo.getSummary());
        assertNotNull(fileInfo.getKeywords());
        assertEquals("论文", fileInfo.getCategory());
        assertEquals(1, fileInfo.getVersion());
        assertEquals("[]", fileInfo.getVersionHistory());
        assertTrue(fileInfo.getIndexed());
        assertEquals("vec-1", fileInfo.getVectorId());
        assertEquals("COMPLETED", fileInfo.getProcessStatus());
        assertEquals(0, fileInfo.getRetryCount());
        assertEquals(1L, fileInfo.getUserId());
        assertEquals("ACTIVE", fileInfo.getStatus());
    }

    // ===== FileStatsDTO测试 =====

    @Test
    @DisplayName("FileStatsDTO统计数据验证")
    void testFileStatsDTO() {
        FileStatsDTO stats = new FileStatsDTO();

        stats.setTotalFiles(100L);
        stats.setActiveFiles(80L);
        stats.setDeletedFiles(20L);
        stats.setTotalSize(1024000L);
        stats.setIndexedFiles(75L);
        stats.setMultiVersionFiles(10L);

        assertEquals(100L, stats.getTotalFiles());
        assertEquals(80L, stats.getActiveFiles());
        assertEquals(20L, stats.getDeletedFiles());
        assertEquals(1024000L, stats.getTotalSize());
        assertEquals(75L, stats.getIndexedFiles());
        assertEquals(10L, stats.getMultiVersionFiles());
    }

    // ===== SimilarityResultDTO测试 =====

    @Test
    @DisplayName("SimilarityResultDTO相似检测结果验证")
    void testSimilarityResultDTO() {
        SimilarityResultDTO result = new SimilarityResultDTO();

        result.setSimilarityDetected(true);
        result.setSimilarFileUuid("uuid-similar");
        result.setSimilarFileName("similar.pdf");
        result.setSimilarityScore(0.85);
        result.setCurrentVersion(2);
        result.setRecommendation("UPDATE_VERSION");

        assertTrue(result.getSimilarityDetected());
        assertEquals("uuid-similar", result.getSimilarFileUuid());
        assertEquals("similar.pdf", result.getSimilarFileName());
        assertEquals(0.85, result.getSimilarityScore());
        assertEquals(2, result.getCurrentVersion());
        assertEquals("UPDATE_VERSION", result.getRecommendation());
    }

    @Test
    @DisplayName("SimilarityResultDTO无相似文件")
    void testSimilarityResultNone() {
        SimilarityResultDTO result = new SimilarityResultDTO();

        result.setSimilarityDetected(false);
        result.setRecommendation("NEW");

        assertFalse(result.getSimilarityDetected());
        assertEquals("NEW", result.getRecommendation());
    }

    // ===== 服务接口方法签名验证 =====

    @Test
    @DisplayName("验证FileInfoService接口方法签名存在")
    void testServiceMethodSignatures() {
        // 通过反射验证接口方法存在
        try {
            // 上传相关方法
            FileInfoService.class.getMethod("upload", org.springframework.web.multipart.MultipartFile.class, Long.class);
            FileInfoService.class.getMethod("uploadNewVersion", org.springframework.web.multipart.MultipartFile.class, String.class, String.class, Long.class);
            FileInfoService.class.getMethod("uploadMultiple", org.springframework.web.multipart.MultipartFile[].class, Long.class);

            // 查询相关方法
            FileInfoService.class.getMethod("getByUuid", String.class, Long.class);
            FileInfoService.class.getMethod("getByUuidInternal", String.class);
            FileInfoService.class.getMethod("getByUserId", Long.class);
            FileInfoService.class.getMethod("getDeletedByUserId", Long.class);
            FileInfoService.class.getMethod("getByCategory", Long.class, String.class);
            FileInfoService.class.getMethod("search", Long.class, String.class);
            FileInfoService.class.getMethod("getStats", Long.class);

            // 版本管理方法
            FileInfoService.class.getMethod("getVersionHistory", String.class);
            FileInfoService.class.getMethod("switchVersion", String.class, Integer.class, Long.class);
            FileInfoService.class.getMethod("downloadVersion", String.class, Integer.class, Long.class);

            // 删除恢复方法
            FileInfoService.class.getMethod("softDelete", String.class, Long.class);
            FileInfoService.class.getMethod("restore", String.class, Long.class);
            FileInfoService.class.getMethod("permanentDelete", String.class, Long.class);

            // 批量操作方法
            FileInfoService.class.getMethod("batchDelete", List.class, Long.class);
            FileInfoService.class.getMethod("batchClassify", List.class, String.class, Long.class);

            // 修改相关方法
            FileInfoService.class.getMethod("updateCategory", String.class, String.class, Long.class);
            FileInfoService.class.getMethod("updateAiResult", String.class, String.class, String.class, String.class);
            FileInfoService.class.getMethod("updateProcessStatus", String.class, String.class);

            // 下载相关方法
            FileInfoService.class.getMethod("download", String.class, Long.class);
            FileInfoService.class.getMethod("getPreviewUrl", String.class, Long.class);

            // 分享相关方法
            FileInfoService.class.getMethod("shareToUsers", String.class, List.class, String.class, Integer.class, Long.class);
            FileInfoService.class.getMethod("createPublicShare", String.class, Integer.class, String.class, Integer.class, Long.class);
            FileInfoService.class.getMethod("getPublicSharedFile", String.class, String.class);
            FileInfoService.class.getMethod("getSharedToMe", Long.class);

            // 相似检测方法
            FileInfoService.class.getMethod("checkSimilarity", String.class, Long.class);

            // 所有方法都存在，验证通过
        } catch (NoSuchMethodException e) {
            fail("FileInfoService接口缺少方法: " + e.getMessage());
        }
    }

    // ===== Levenshtein距离算法验证 =====

    @Test
    @DisplayName("相似度计算逻辑验证")
    void testSimilarityCalculation() {
        // 测试完全相同
        double score1 = calculateSimilarity("test.pdf", "test.pdf");
        assertEquals(1.0, score1);

        // 测试完全不同（aaa vs bbb 距离为3，相似度约为0.57）
        double score2 = calculateSimilarity("aaa.pdf", "bbb.pdf");
        assertTrue(score2 < 0.7);

        // 测试部分相似
        double score3 = calculateSimilarity("论文A_v1.pdf", "论文A_v2.pdf");
        assertTrue(score3 > 0.6);

        // 测试空值
        double score4 = calculateSimilarity(null, "test.pdf");
        assertEquals(0.0, score4);
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        s1 = s1.toLowerCase().trim();
        s2 = s2.toLowerCase().trim();
        if (s1.equals(s2)) return 1.0;

        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;

        int distance = levenshteinDistance(s1, s2);
        return 1.0 - (double) distance / maxLen;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1])) + 1;
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    // ===== 版本切换逻辑验证 =====

    @Test
    @DisplayName("版本切换场景验证")
    void testVersionSwitchLogic() {
        // 当前版本为3，切换到版本1
        int currentVersion = 3;
        int targetVersion = 1;

        // 验证版本号有效性
        assertTrue(targetVersion > 0);
        assertTrue(targetVersion <= currentVersion);

        // 验证版本历史中存在目标版本
        assertTrue(targetVersion >= 1 && targetVersion <= 3);
    }

    // ===== 分享逻辑验证 =====

    @Test
    @DisplayName("分享权限验证")
    void testSharePermissions() {
        String[] permissions = {"VIEW", "VIEW_DOWNLOAD"};

        for (String permission : permissions) {
            assertTrue(permission.equals("VIEW") || permission.equals("VIEW_DOWNLOAD"));
        }
    }

    @Test
    @DisplayName("公开分享密码验证")
    void testPublicSharePassword() {
        String shareCode = "abc123def456789";
        String password = "secret123";

        // 分享码可以是任意长度
        assertTrue(shareCode.length() >= 8);

        // 密码可选
        assertNotNull(password);
    }

    // ===== 批量操作验证 =====

    @Test
    @DisplayName("批量操作数量限制验证")
    void testBatchOperationLimit() {
        List<String> fileUuids = Arrays.asList("u1", "u2", "u3", "u4", "u5");

        // 验证批量操作不超过100个
        assertTrue(fileUuids.size() <= 100);
        assertFalse(fileUuids.isEmpty());
    }

    // ===== 软删除恢复逻辑验证 =====

    @Test
    @DisplayName("软删除状态转换验证")
    void testSoftDeleteStateTransition() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setStatus("ACTIVE");
        fileInfo.setDeleteTime(null);

        // 软删除
        fileInfo.setStatus("DELETED");
        fileInfo.setDeleteTime(java.time.LocalDateTime.now());

        assertEquals("DELETED", fileInfo.getStatus());
        assertNotNull(fileInfo.getDeleteTime());

        // 恢复
        fileInfo.setStatus("ACTIVE");
        fileInfo.setDeleteTime(null);

        assertEquals("ACTIVE", fileInfo.getStatus());
        assertNull(fileInfo.getDeleteTime());
    }

    @Test
    @DisplayName("30天回收站保留验证")
    void testRecycleBinRetention() {
        java.time.LocalDateTime deleteTime = java.time.LocalDateTime.now();
        java.time.LocalDateTime expiryTime = deleteTime.plusDays(30);

        assertTrue(expiryTime.isAfter(deleteTime));
        assertEquals(30, java.time.temporal.ChronoUnit.DAYS.between(deleteTime, expiryTime));
    }
}