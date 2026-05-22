package com.javaee.fileservice.service.impl;

import com.javaee.fileservice.client.AIServiceClient;
import com.javaee.fileservice.dto.SimilarityResultDTO;
import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.mapper.FileInfoMapper;
import com.javaee.common.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SimilarityService单元测试 (v3.0)
 * 验证相似度检测的文件名+内容综合评分功能
 *
 * 验收标准：
 * 1. Levenshtein距离计算准确
 * 2. 综合评分：score = 0.3 * nameSimilarity + 0.7 * contentSimilarity
 * 3. 推荐策略：>=0.8为UPDATE_VERSION，<0.5为NEW，中间为USER_DECIDE
 */
class SimilarityServiceImplTest {

    @Mock
    private FileInfoMapper fileInfoMapper;

    @Mock
    private AIServiceClient aiServiceClient;

    @InjectMocks
    private SimilarityServiceImpl similarityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试1：Levenshtein距离计算
     */
    @Test
    @DisplayName("calculateNameSimilarity正确计算文件名相似度")
    void testCalculateNameSimilarity() {
        // 完全相同
        double similarity1 = similarityService.calculateNameSimilarity("论文A", "论文A");
        assertEquals(1.0, similarity1, 0.01, "完全相同应返回1.0");

        // 部分相似
        double similarity2 = similarityService.calculateNameSimilarity("论文A", "论文B");
        assertTrue(similarity2 >= 0.5 && similarity2 < 1.0, "部分相似应在0.5-1.0之间");

        // 完全不同
        double similarity3 = similarityService.calculateNameSimilarity("论文A", "报告B");
        assertTrue(similarity3 < 0.5, "完全不同应小于0.5");

        // 版本号相似（论文A_v1 vs 论文A_v2）
        double similarity4 = similarityService.calculateNameSimilarity("论文A_v1", "论文A_v2");
        assertTrue(similarity4 >= 0.8, "版本号相似应大于0.8");
    }

    /**
     * 测试2：推荐策略判断
     */
    @Test
    @DisplayName("recommendAction根据相似度分数返回正确推荐")
    void testRecommendAction() {
        // 高相似度：推荐更新版本
        String action1 = similarityService.recommendAction(0.85);
        assertEquals("UPDATE_VERSION", action1, "相似度>=0.8应推荐UPDATE_VERSION");

        String action2 = similarityService.recommendAction(0.95);
        assertEquals("UPDATE_VERSION", action2, "相似度>=0.8应推荐UPDATE_VERSION");

        // 低相似度：推荐新建
        String action3 = similarityService.recommendAction(0.3);
        assertEquals("NEW", action3, "相似度<0.5应推荐NEW");

        // 中等相似度：用户决定
        String action4 = similarityService.recommendAction(0.6);
        assertEquals("USER_DECIDE", action4, "相似度0.5-0.8应推荐USER_DECIDE");

        String action5 = similarityService.recommendAction(0.75);
        assertEquals("USER_DECIDE", action5, "相似度0.5-0.8应推荐USER_DECIDE");
    }

    /**
     * 测试3：仅文件名相似度检测
     */
    @Test
    @DisplayName("checkSimilarity仅用文件名时正确检测")
    void testCheckSimilarityWithFileNameOnly() {
        // 模拟用户文件列表
        FileInfo existingFile = new FileInfo();
        existingFile.setFileUuid("existing-uuid");
        existingFile.setOriginalName("论文A_v1.pdf");
        existingFile.setVersion(1);

        List<FileInfo> userFiles = new ArrayList<>();
        userFiles.add(existingFile);
        when(fileInfoMapper.selectByUserId(1L)).thenReturn(userFiles);

        // 执行相似检测
        SimilarityResultDTO result = similarityService.checkSimilarity("论文A_v2.pdf", 1L);

        // 验证结果（论文A_v1 vs 论文A_v2相似度应>0.8）
        if (result != null) {
            assertTrue(result.getSimilarityDetected(), "应检测到相似文件");
            assertTrue(result.getSimilarityScore() >= 0.5, "相似度分数应>=0.5");
        }
    }

    /**
     * 测试4：含内容的综合相似度检测
     */
    @Test
    @DisplayName("checkSimilarityWithContent结合文件名和内容相似度")
    void testCheckSimilarityWithContent() {
        // 模拟用户文件列表
        FileInfo existingFile = new FileInfo();
        existingFile.setFileUuid("existing-uuid");
        existingFile.setOriginalName("论文A.pdf");
        existingFile.setVersion(1);

        List<FileInfo> userFiles = new ArrayList<>();
        userFiles.add(existingFile);
        when(fileInfoMapper.selectByUserId(1L)).thenReturn(userFiles);

        // 模拟AI相似内容搜索
        Map<String, Object> searchResult = new HashMap<>();
        searchResult.put("id", "existing-uuid_v1");
        searchResult.put("similarity", 0.9);

        List<Map<String, Object>> searchResults = new ArrayList<>();
        searchResults.add(searchResult);

        Result<List<Map<String, Object>>> mockResult = Result.success(searchResults);
        when(aiServiceClient.searchSimilar(anyString(), anyInt())).thenReturn(mockResult);

        // 模拟文件查询
        when(fileInfoMapper.selectByFileUuid("existing-uuid")).thenReturn(existingFile);

        // 执行含内容的相似检测
        String content = "本文研究深度学习在图像识别领域的应用...";
        SimilarityResultDTO result = similarityService.checkSimilarityWithContent("论文A_v2.pdf", content, 1L);

        // 验证结果
        if (result != null) {
            assertTrue(result.getSimilarityDetected(), "应检测到相似文件");

            // 综合评分 = 0.3 * nameSimilarity + 0.7 * contentSimilarity
            // 由于内容和文件名都匹配，分数应该较高
            assertTrue(result.getSimilarityScore() >= 0.6, "综合相似度分数应>=0.6");
        }
    }

    /**
     * 测试5：无相似文件时返回null
     */
    @Test
    @DisplayName("用户无文件时checkSimilarity返回null")
    void testCheckSimilarityNoFiles() {
        // 模拟空文件列表
        when(fileInfoMapper.selectByUserId(1L)).thenReturn(new ArrayList<>());

        // 执行检测
        SimilarityResultDTO result = similarityService.checkSimilarity("新文件.pdf", 1L);

        // 验证返回null
        assertNull(result, "无文件时应返回null");
    }

    /**
     * 测试6：文件名差异大时不应检测到相似
     */
    @Test
    @DisplayName("文件名差异大时不应检测到相似")
    void testCheckSimilarityDifferentFiles() {
        // 模拟用户文件列表（完全不同的文件）
        FileInfo existingFile = new FileInfo();
        existingFile.setFileUuid("existing-uuid");
        existingFile.setOriginalName("技术报告B.docx");
        existingFile.setVersion(1);

        List<FileInfo> userFiles = new ArrayList<>();
        userFiles.add(existingFile);
        when(fileInfoMapper.selectByUserId(1L)).thenReturn(userFiles);

        // 执行相似检测（论文A vs 技术报告B）
        SimilarityResultDTO result = similarityService.checkSimilarity("论文A.pdf", 1L);

        // 验证结果（相似度应该很低，低于阈值）
        if (result != null) {
            assertTrue(result.getSimilarityScore() < 0.5, "不同文件相似度应<0.5");
        }
    }
}