package com.javaee.fileservice.service;

import com.javaee.fileservice.dto.SimilarityResultDTO;

import java.util.List;

/**
 * 文件相似度检测服务接口（v2.0新增）
 */
public interface FileSimilarityService {

    /**
     * 检测文件相似度
     * 根据文件名和类型检测用户已有文件中是否存在相似文件
     * @param newFileName 新文件名
     * @param newFileType 新文件类型（MIME）
     * @param userId 用户ID
     * @return 相似度检测结果（如果没有相似文件返回null）
     */
    SimilarityResultDTO detectSimilarity(String newFileName, String newFileType, Long userId);

    /**
     * 计算两个字符串的Levenshtein距离
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 编辑距离
     */
    int levenshteinDistance(String s1, String s2);

    /**
     * 计算文件名相似度
     * @param name1 文件名1
     * @param name2 文件名2
     * @return 相似度 (0.0 - 1.0)
     */
    double calculateNameSimilarity(String name1, String name2);
}