package com.javaee.fileservice.service;

import com.javaee.fileservice.dto.SimilarityResultDTO;

/**
 * 相似度检测服务 (v3.0)
 * 统一的文件相似度检测接口
 * 整合文件名相似度和内容向量相似度
 */
public interface SimilarityService {

    /**
     * 检测文件相似度
     * @param fileName 待检测文件名
     * @param userId 用户ID
     * @return 相似度检测结果
     */
    SimilarityResultDTO checkSimilarity(String fileName, Long userId);

    /**
     * 检测文件相似度（带内容）
     * v3.0增强：结合内容向量相似度进行综合评分
     * @param fileName 待检测文件名
     * @param content 文件内容（用于向量相似度检测）
     * @param userId 用户ID
     * @return 相似度检测结果
     */
    SimilarityResultDTO checkSimilarityWithContent(String fileName, String content, Long userId);

    /**
     * 计算文件名Levenshtein相似度
     * @param name1 文件名1
     * @param name2 文件名2
     * @return 相似度分数 (0.0 - 1.0)
     */
    double calculateNameSimilarity(String name1, String name2);

    /**
     * 根据综合相似度推荐处理方式
     * @param similarityScore 综合相似度分数
     * @return 推荐处理方式: NEW / UPDATE_VERSION / USER_DECIDE
     */
    String recommendAction(double similarityScore);
}