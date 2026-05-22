package com.javaee.fileservice.service.impl;

import com.javaee.fileservice.dto.SimilarityResultDTO;
import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.service.FileInfoService;
import com.javaee.fileservice.service.FileSimilarityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文件相似度检测服务实现（v2.0新增）
 *
 * 相似度计算公式：
 * 综合相似度 = 文件名相似度 * 0.6 + 文件类型匹配 * 0.4
 *
 * 推荐策略：
 * - 相似度 > 0.8: 推荐UPDATE_VERSION
 * - 相似度 < 0.5: 推荐NEW
 * - 0.5 ~ 0.8: 用户自行决定
 */
@Service
public class FileSimilarityServiceImpl implements FileSimilarityService {

    private static final Logger log = LoggerFactory.getLogger(FileSimilarityServiceImpl.class);

    // 相似度阈值
    private static final double HIGH_SIMILARITY_THRESHOLD = 0.8;   // 高相似度，推荐更新版本
    private static final double LOW_SIMILARITY_THRESHOLD = 0.5;    // 低相似度，推荐新建

    // 权重配置
    private static final double NAME_WEIGHT = 0.6;   // 文件名相似度权重
    private static final double TYPE_WEIGHT = 0.4;   // 文件类型匹配权重

    @Autowired
    private FileInfoService fileInfoService;

    @Override
    public SimilarityResultDTO detectSimilarity(String newFileName, String newFileType, Long userId) {
        log.info("检测文件相似度: newFileName={}, newFileType={}, userId={}", newFileName, newFileType, userId);

        // 获取用户所有文件
        List<FileInfo> existingFiles = fileInfoService.getByUserId(userId);

        if (existingFiles == null || existingFiles.isEmpty()) {
            log.info("用户没有已有文件，无需检测相似度");
            return null;
        }

        // 提取纯文件名（去掉扩展名）用于比较
        String newBaseName = extractBaseName(newFileName);
        String newExtension = extractExtension(newFileName);

        SimilarityResultDTO bestMatch = null;
        double bestScore = 0;

        for (FileInfo existing : existingFiles) {
            String existingFileName = existing.getOriginalName();
            if (existingFileName == null) continue;

            String existingBaseName = extractBaseName(existingFileName);
            String existingExtension = extractExtension(existingFileName);

            // 计算文件名相似度
            double nameSimilarity = calculateNameSimilarity(newBaseName, existingBaseName);

            // 计算文件类型匹配度
            boolean typeMatch = isTypeMatch(newExtension, existingExtension);

            // 综合相似度
            double overallScore = nameSimilarity * NAME_WEIGHT + (typeMatch ? TYPE_WEIGHT : 0);

            log.debug("对比文件: {} vs {}, nameSimilarity={}, typeMatch={}, overallScore={}",
                     newFileName, existingFileName, nameSimilarity, typeMatch, overallScore);

            // 只保留最高相似度的匹配
            if (overallScore > bestScore && overallScore > LOW_SIMILARITY_THRESHOLD) {
                bestScore = overallScore;
                bestMatch = createResult(existing, overallScore, nameSimilarity, typeMatch);
            }
        }

        if (bestMatch != null) {
            log.info("找到相似文件: fileUuid={}, fileName={}, score={}",
                    bestMatch.getSimilarFileUuid(), bestMatch.getSimilarFileName(), bestMatch.getSimilarityScore());
        } else {
            log.info("未找到相似文件，推荐新建文档");
        }

        return bestMatch;
    }

    /**
     * 创建检测结果DTO
     */
    private SimilarityResultDTO createResult(FileInfo file, double score,
                                              double nameSimilarity, boolean typeMatch) {
        SimilarityResultDTO result = new SimilarityResultDTO();
        result.setSimilarityDetected(true);
        result.setSimilarFileUuid(file.getFileUuid());
        result.setSimilarFileName(file.getOriginalName());
        result.setSimilarityScore(score);
        result.setCurrentVersion(file.getVersion() != null ? file.getVersion() : 1);

        // 设置推荐操作
        if (score >= HIGH_SIMILARITY_THRESHOLD) {
            result.setRecommendation("UPDATE_VERSION");
        } else if (score < LOW_SIMILARITY_THRESHOLD) {
            result.setRecommendation("NEW");
        } else {
            result.setRecommendation("USER_DECIDE");
        }

        return result;
    }

    @Override
    public int levenshteinDistance(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";

        int len1 = s1.length();
        int len2 = s2.length();

        // 使用动态规划计算编辑距离
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                char c1 = s1.charAt(i - 1);
                char c2 = s2.charAt(j - 1);

                if (c1 == c2) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                        dp[i - 1][j - 1] + 1,  // 替换
                        Math.min(
                            dp[i][j - 1] + 1,    // 插入
                            dp[i - 1][j] + 1     // 删除
                        )
                    );
                }
            }
        }

        return dp[len1][len2];
    }

    @Override
    public double calculateNameSimilarity(String name1, String name2) {
        if (name1 == null || name2 == null) return 0;

        int distance = levenshteinDistance(name1.toLowerCase(), name2.toLowerCase());
        int maxLen = Math.max(name1.length(), name2.length());

        if (maxLen == 0) return 1.0;

        double similarity = 1.0 - (double) distance / maxLen;
        return Math.max(0, Math.min(1, similarity));
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
     * 提取文件扩展名
     */
    private String extractExtension(String fileName) {
        if (fileName == null) return "";

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 判断文件类型是否匹配
     */
    private boolean isTypeMatch(String ext1, String ext2) {
        if (ext1 == null || ext2 == null) return false;

        // 直接匹配扩展名
        if (ext1.equals(ext2)) return true;

        // 同类文件类型匹配（如doc和docx）
        String[] docTypes = {"doc", "docx"};
        String[] pdfTypes = {"pdf"};
        String[] txtTypes = {"txt", "md"};
        String[] xlsTypes = {"xls", "xlsx"};
        String[] pptTypes = {"ppt", "pptx"};

        return isSameCategory(ext1, ext2, docTypes) ||
               isSameCategory(ext1, ext2, pdfTypes) ||
               isSameCategory(ext1, ext2, txtTypes) ||
               isSameCategory(ext1, ext2, xlsTypes) ||
               isSameCategory(ext1, ext2, pptTypes);
    }

    /**
     * 判断是否属于同一类别
     */
    private boolean isSameCategory(String ext1, String ext2, String[] category) {
        boolean found1 = false, found2 = false;
        for (String ext : category) {
            if (ext.equals(ext1)) found1 = true;
            if (ext.equals(ext2)) found2 = true;
        }
        return found1 && found2;
    }
}