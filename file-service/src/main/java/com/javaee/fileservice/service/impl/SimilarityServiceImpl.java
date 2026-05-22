package com.javaee.fileservice.service.impl;

import com.javaee.fileservice.client.AIServiceClient;
import com.javaee.fileservice.dto.SimilarityResultDTO;
import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.mapper.FileInfoMapper;
import com.javaee.fileservice.service.SimilarityService;
import com.javaee.common.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 相似度检测服务实现 (v3.0)
 * 统一的文件相似度检测
 * 整合文件名Levenshtein相似度和内容向量相似度
 *
 * 综合评分公式：
 * score = 0.3 * fileNameSimilarity + 0.7 * contentSimilarity
 *
 * 推荐策略：
 * - score >= 0.8: UPDATE_VERSION
 * - score < 0.5: NEW
 * - 0.5 ~ 0.8: USER_DECIDE
 */
@Service
public class SimilarityServiceImpl implements SimilarityService {

    private static final Logger log = LoggerFactory.getLogger(SimilarityServiceImpl.class);

    // 相似度阈值
    private static final double HIGH_THRESHOLD = 0.8;
    private static final double LOW_THRESHOLD = 0.5;

    // 权重配置（v3.0：内容相似度权重更高）
    private static final double NAME_WEIGHT = 0.3;
    private static final double CONTENT_WEIGHT = 0.7;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private AIServiceClient aiServiceClient;

    @Override
    public SimilarityResultDTO checkSimilarity(String fileName, Long userId) {
        log.info("检测文件相似度（仅文件名）: fileName={}, userId={}", fileName, userId);

        // 获取用户所有活跃文件
        List<FileInfo> existingFiles = fileInfoMapper.selectByUserId(userId);

        if (existingFiles == null || existingFiles.isEmpty()) {
            return null;
        }

        String newBaseName = extractBaseName(fileName);
        SimilarityResultDTO bestMatch = null;
        double bestScore = 0;

        for (FileInfo existing : existingFiles) {
            String existingFileName = existing.getOriginalName();
            if (existingFileName == null) continue;

            String existingBaseName = extractBaseName(existingFileName);
            double nameSimilarity = calculateNameSimilarity(newBaseName, existingBaseName);

            // 仅使用文件名相似度（无内容）
            double overallScore = nameSimilarity;

            if (overallScore > bestScore && overallScore > LOW_THRESHOLD) {
                bestScore = overallScore;
                bestMatch = createResult(existing, overallScore, nameSimilarity, 0);
            }
        }

        return bestMatch;
    }

    @Override
    public SimilarityResultDTO checkSimilarityWithContent(String fileName, String content, Long userId) {
        log.info("检测文件相似度（含内容）: fileName={}, userId={}", fileName, userId);

        // 获取用户所有活跃文件
        List<FileInfo> existingFiles = fileInfoMapper.selectByUserId(userId);

        if (existingFiles == null || existingFiles.isEmpty()) {
            return null;
        }

        String newBaseName = extractBaseName(fileName);

        // v3.0：调用ai-service进行向量相似度检索
        double contentSimilarity = 0;
        FileInfo contentMatchFile = null;

        if (content != null && !content.trim().isEmpty()) {
            try {
                // 调用混合检索获取相似内容（使用现有search接口）
                Result<List<Map<String, Object>>> searchResult =
                    aiServiceClient.searchSimilar(content, 3);

                if (searchResult != null && searchResult.getCode() == 200) {
                    List<Map<String, Object>> results = searchResult.getData();
                    if (results != null && !results.isEmpty()) {
                        Map<String, Object> topResult = results.get(0);
                        String matchedId = (String) topResult.get("id");
                        // 解析版本后缀ID：fileUuid_v版本号 -> fileUuid
                        String matchedFileUuid = matchedId.split("_v")[0];
                        Object similarityObj = topResult.get("similarity");
                        contentSimilarity = similarityObj != null ?
                            ((Number) similarityObj).doubleValue() : 0;

                        contentMatchFile = fileInfoMapper.selectByFileUuid(matchedFileUuid);
                    }
                }
            } catch (Exception e) {
                log.warn("内容相似度检测失败: {}", e.getMessage());
            }
        }

        // 文件名相似度检测
        SimilarityResultDTO bestMatch = null;
        double bestScore = 0;

        for (FileInfo existing : existingFiles) {
            String existingFileName = existing.getOriginalName();
            if (existingFileName == null) continue;

            String existingBaseName = extractBaseName(existingFileName);
            double nameSimilarity = calculateNameSimilarity(newBaseName, existingBaseName);

            // v3.0：综合评分（内容优先）
            double overallScore;
            if (contentMatchFile != null &&
                existing.getFileUuid().equals(contentMatchFile.getFileUuid())) {
                // 文件名匹配且内容匹配，使用加权综合评分
                overallScore = NAME_WEIGHT * nameSimilarity + CONTENT_WEIGHT * contentSimilarity;
            } else {
                // 仅文件名匹配
                overallScore = nameSimilarity * (NAME_WEIGHT + CONTENT_WEIGHT);
            }

            if (overallScore > bestScore && overallScore > LOW_THRESHOLD) {
                bestScore = overallScore;
                bestMatch = createResult(existing, overallScore, nameSimilarity, contentSimilarity);
            }
        }

        if (bestMatch != null) {
            log.info("相似度检测结果: fileUuid={}, score={}, nameSim={}, contentSim={}",
                    bestMatch.getSimilarFileUuid(), bestMatch.getSimilarityScore(),
                    nameSimilarityOf(bestMatch), contentSimilarity);
        }

        return bestMatch;
    }

    /**
     * 创建检测结果DTO
     */
    private SimilarityResultDTO createResult(FileInfo file, double score,
                                             double nameSimilarity, double contentSimilarity) {
        SimilarityResultDTO result = new SimilarityResultDTO();
        result.setSimilarityDetected(true);
        result.setSimilarFileUuid(file.getFileUuid());
        result.setSimilarFileName(file.getOriginalName());
        result.setSimilarityScore(score);
        result.setCurrentVersion(file.getVersion() != null ? file.getVersion() : 1);
        result.setRecommendation(recommendAction(score));

        return result;
    }

    @Override
    public double calculateNameSimilarity(String name1, String name2) {
        if (name1 == null || name2 == null) return 0;

        int distance = levenshteinDistance(name1.toLowerCase(), name2.toLowerCase());
        int maxLen = Math.max(name1.length(), name2.length());

        if (maxLen == 0) return 1.0;

        return Math.max(0, Math.min(1, 1.0 - (double) distance / maxLen));
    }

    /**
     * Levenshtein编辑距离算法
     */
    private int levenshteinDistance(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";

        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                char c1 = s1.charAt(i - 1);
                char c2 = s2.charAt(j - 1);

                if (c1 == c2) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                        dp[i - 1][j - 1] + 1,
                        Math.min(dp[i][j - 1] + 1, dp[i - 1][j] + 1)
                    );
                }
            }
        }

        return dp[len1][len2];
    }

    @Override
    public String recommendAction(double similarityScore) {
        if (similarityScore >= HIGH_THRESHOLD) {
            return "UPDATE_VERSION";
        } else if (similarityScore < LOW_THRESHOLD) {
            return "NEW";
        } else {
            return "USER_DECIDE";
        }
    }

    /**
     * 提取文件基本名（去掉扩展名）
     */
    private String extractBaseName(String fileName) {
        if (fileName == null) return "";

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }

    /**
     * 从result中提取nameSimilarity（辅助方法）
     */
    private double nameSimilarityOf(SimilarityResultDTO result) {
        // 可以扩展DTO添加更多字段
        return result.getSimilarityScore() * NAME_WEIGHT / (NAME_WEIGHT + CONTENT_WEIGHT);
    }
}