package com.javaee.fileservice.service;

import com.javaee.fileservice.dto.*;
import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.entity.VersionHistoryItem;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件信息服务接口 (v3.0)
 * 统一的文件管理服务，整合原file-service和document-service功能
 */
public interface FileInfoService {

    // ===== 上传相关 =====

    /**
     * 上传新文件（创建新FileInfo）
     * @param file 文件
     * @param userId 用户ID
     * @return FileInfo
     */
    FileInfo upload(MultipartFile file, Long userId);

    /**
     * 上传新版本（更新现有文件的版本）
     * @param file 新版本文件
     * @param existingFileUuid 现有文件UUID
     * @param changeSummary 变更说明
     * @param userId 用户ID
     * @return FileInfo
     */
    FileInfo uploadNewVersion(MultipartFile file, String existingFileUuid, String changeSummary, Long userId);

    /**
     * 批量上传
     * @param files 文件数组
     * @param userId 用户ID
     * @return FileInfo列表
     */
    List<FileInfo> uploadMultiple(MultipartFile[] files, Long userId);

    // ===== 查询相关 =====

    /**
     * 根据UUID获取文件（需验证所有权）
     * @param fileUuid 文件UUID
     * @param userId 用户ID
     * @return FileInfo
     */
    FileInfo getByUuid(String fileUuid, Long userId);

    /**
     * 根据UUID获取文件（内部使用，不验证权限）
     * @param fileUuid 文件UUID
     * @return FileInfo
     */
    FileInfo getByUuidInternal(String fileUuid);

    /**
     * 获取用户文件列表
     * @param userId 用户ID
     * @return FileInfo列表
     */
    List<FileInfo> getByUserId(Long userId);

    /**
     * 获取用户已删除文件列表
     * @param userId 用户ID
     * @return FileInfo列表
     */
    List<FileInfo> getDeletedByUserId(Long userId);

    /**
     * 根据分类获取文件
     * @param userId 用户ID
     * @param category 分类
     * @return FileInfo列表
     */
    List<FileInfo> getByCategory(Long userId, String category);

    /**
     * 搜索文件
     * @param userId 用户ID
     * @param keyword 关键词
     * @return FileInfo列表
     */
    List<FileInfo> search(Long userId, String keyword);

    /**
     * 获取用户文件统计
     * @param userId 用户ID
     * @return 统计数据
     */
    FileStatsDTO getStats(Long userId);

    // ===== 版本管理 =====

    /**
     * 获取版本历史
     * @param fileUuid 文件UUID
     * @return 版本历史列表
     */
    List<VersionHistoryItem> getVersionHistory(String fileUuid);

    /**
     * 切换到指定版本
     * @param fileUuid 文件UUID
     * @param targetVersion 目标版本号
     * @param userId 用户ID
     * @return FileInfo
     */
    FileInfo switchVersion(String fileUuid, Integer targetVersion, Long userId);

    /**
     * 获取指定版本的历史文件下载
     * @param fileUuid 文件UUID
     * @param version 版本号
     * @param userId 用户ID
     * @return 文件字节
     */
    byte[] downloadVersion(String fileUuid, Integer version, Long userId);

    // ===== 删除恢复 =====

    /**
     * 软删除文件
     * @param fileUuid 文件UUID
     * @param userId 用户ID
     */
    void softDelete(String fileUuid, Long userId);

    /**
     * 恢复文件
     * @param fileUuid 文件UUID
     * @param userId 用户ID
     */
    void restore(String fileUuid, Long userId);

    /**
     * 永久删除
     * @param fileUuid 文件UUID
     * @param userId 用户ID
     */
    void permanentDelete(String fileUuid, Long userId);

    // ===== 批量操作 =====

    /**
     * 批量删除
     * @param fileUuids 文件UUID列表
     * @param userId 用户ID
     */
    void batchDelete(List<String> fileUuids, Long userId);

    /**
     * 批量修改分类
     * @param fileUuids 文件UUID列表
     * @param category 新分类
     * @param userId 用户ID
     */
    void batchClassify(List<String> fileUuids, String category, Long userId);

    // ===== 修改相关 =====

    /**
     * 更新分类
     * @param fileUuid 文件UUID
     * @param category 分类
     * @param userId 用户ID
     */
    void updateCategory(String fileUuid, String category, Long userId);

    /**
     * 更新AI结果
     * @param fileUuid 文件UUID
     * @param summary 摘要
     * @param keywords 关键词
     * @param category 分类
     */
    void updateAiResult(String fileUuid, String summary, String keywords, String category);

    /**
     * 更新处理状态
     * @param fileUuid 文件UUID
     * @param status 状态
     */
    void updateProcessStatus(String fileUuid, String status);

    // ===== 下载相关 =====

    /**
     * 下载当前版本文件
     * @param fileUuid 文件UUID
     * @param userId 用户ID
     * @return 文件字节
     */
    byte[] download(String fileUuid, Long userId);

    /**
     * 获取预览URL
     * @param fileUuid 文件UUID
     * @param userId 用户ID
     * @return 预览URL
     */
    String getPreviewUrl(String fileUuid, Long userId);

    // ===== 分享相关 =====

    /**
     * 分享给其他用户
     * @param fileUuid 文件UUID
     * @param shareToIds 分享给的用户ID列表
     * @param permission 权限（VIEW/VIEW_DOWNLOAD）
     * @param expireDays 过期天数
     * @param userId 操作用户ID
     */
    void shareToUsers(String fileUuid, List<Long> shareToIds, String permission, Integer expireDays, Long userId);

    /**
     * 创建公开分享链接
     * @param fileUuid 文件UUID
     * @param expireDays 过期天数
     * @param password 密码（可选）
     * @param downloadLimit 下载限制
     * @param userId 操作用户ID
     * @return 分享码
     */
    String createPublicShare(String fileUuid, Integer expireDays, String password, Integer downloadLimit, Long userId);

    /**
     * 获取公开分享的文件
     * @param shareCode 分享码
     * @param password 密码（如有）
     * @return FileInfo
     */
    FileInfo getPublicSharedFile(String shareCode, String password);

    /**
     * 获取分享给我的文件列表
     * @param userId 用户ID
     * @return FileInfo列表
     */
    List<FileInfo> getSharedToMe(Long userId);

    // ===== 相似检测 =====

    /**
     * 检测文件相似度
     * @param fileName 文件名
     * @param userId 用户ID
     * @return 相似检测结果
     */
    SimilarityResultDTO checkSimilarity(String fileName, Long userId);

    // ===== 重新处理 =====

    /**
     * 重新处理文件（重新索引向量）
     * @param fileUuid 文件UUID
     * @param userId 用户ID
     */
    void reprocessFile(String fileUuid, Long userId);

    /**
     * 批量重新处理未索引文件
     * @param userId 用户ID
     * @return 处理数量
     */
    int batchReprocessUnindexed(Long userId);
}