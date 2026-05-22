package com.javaee.fileservice.service.impl;

import com.javaee.fileservice.client.AIServiceClient;
import com.javaee.fileservice.dto.FileStatsDTO;
import com.javaee.fileservice.dto.SimilarityResultDTO;
import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.entity.FileShare;
import com.javaee.fileservice.entity.PublicShare;
import com.javaee.fileservice.entity.VersionHistoryItem;
import com.javaee.fileservice.mapper.FileInfoMapper;
import com.javaee.fileservice.mapper.FileShareMapper;
import com.javaee.fileservice.mapper.PublicShareMapper;
import com.javaee.fileservice.service.FileInfoService;
import com.javaee.fileservice.service.FileProcessService;
import com.javaee.fileservice.service.SimilarityService;
import com.javaee.fileservice.util.VersionHistoryParser;
import com.javaee.fileservice.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 文件信息服务实现 (v3.0)
 */
@Service
public class FileInfoServiceImpl implements FileInfoService {

    private static final Logger log = LoggerFactory.getLogger(FileInfoServiceImpl.class);

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private FileShareMapper fileShareMapper;

    @Autowired
    private PublicShareMapper publicShareMapper;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private AIServiceClient aiServiceClient;

    @Autowired
    private SimilarityService similarityService;

    @Autowired
    private FileProcessService fileProcessService;

    // ===== 上传相关 =====

    @Override
    @Transactional
    public FileInfo upload(MultipartFile file, Long userId) {
        log.info("上传新文件: fileName={}, userId={}", file.getOriginalFilename(), userId);

        try {
            // 生成UUID
            String fileUuid = UUID.randomUUID().toString();
            String originalName = file.getOriginalFilename();
            String fileName = generateStorageFileName(fileUuid, originalName);
            String storagePath = minioConfig.getBucket() + "/" + fileName;
            String fileType = getFileType(originalName);
            Long fileSize = file.getSize();

            // 上传到MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(fileName)
                            .stream(file.getInputStream(), fileSize, -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 创建FileInfo
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileUuid(fileUuid);
            fileInfo.setFileName(fileName);
            fileInfo.setOriginalName(originalName);
            fileInfo.setFileType(fileType);
            fileInfo.setFileSize(fileSize);
            fileInfo.setStoragePath(storagePath);
            fileInfo.setVersion(1);
            fileInfo.setVersionHistory(null);
            fileInfo.setIndexed(false);  // v3.0 fix: 初始设置为false，处理链完成后设置为true
            fileInfo.setProcessStatus("PENDING");
            fileInfo.setRetryCount(0);
            fileInfo.setUserId(userId);
            fileInfo.setStatus("ACTIVE");
            fileInfo.setCreateTime(LocalDateTime.now());

            fileInfoMapper.insert(fileInfo);

            // v3.0：异步触发处理链进行AI处理和向量索引
            fileProcessService.processFile(fileUuid, userId);

            log.info("文件上传成功: fileUuid={}, id={}", fileUuid, fileInfo.getId());
            return fileInfo;

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public FileInfo uploadNewVersion(MultipartFile file, String existingFileUuid, String changeSummary, Long userId) {
        log.info("上传新版本: fileUuid={}, userId={}", existingFileUuid, userId);

        FileInfo existingFile = getByUuid(existingFileUuid, userId);
        if (existingFile == null) {
            throw new RuntimeException("文件不存在: " + existingFileUuid);
        }

        try {
            String newStorageName = generateStorageFileName(UUID.randomUUID().toString(), file.getOriginalFilename());
            String newStoragePath = minioConfig.getBucket() + "/" + newStorageName;

            // 上传新版本到MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(newStorageName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // v3.0：软删除旧版本向量
            int oldVersion = existingFile.getVersion();
            try {
                aiServiceClient.softDeleteVector(existingFileUuid + "_v" + oldVersion, String.valueOf(userId));
                log.info("旧版本向量软删除: fileUuid={}, version={}", existingFileUuid, oldVersion);
            } catch (Exception e) {
                log.warn("旧版本向量软删除失败: {}", e.getMessage());
            }

            // 保存当前版本到历史
            VersionHistoryItem historyItem = VersionHistoryParser.buildItem(
                    existingFile.getVersion(),
                    existingFile.getFileUuid(),
                    existingFile.getOriginalName(),
                    existingFile.getStoragePath(),
                    existingFile.getFileSize(),
                    existingFile.getSummary(),
                    VersionHistoryParser.parseKeywords(existingFile.getKeywords()),
                    existingFile.getCategory(),
                    existingFile.getVersion() == 1 ? "初始版本" : "版本" + existingFile.getVersion(),
                    existingFile.getCreateTime()
            );

            String newHistoryJson = VersionHistoryParser.addVersion(existingFile.getVersionHistory(), historyItem);

            // 更新FileInfo
            existingFile.setFileName(newStorageName);
            existingFile.setOriginalName(file.getOriginalFilename());
            existingFile.setFileSize(file.getSize());
            existingFile.setStoragePath(newStoragePath);
            existingFile.setVersion(existingFile.getVersion() + 1);
            existingFile.setVersionHistory(newHistoryJson);
            existingFile.setSummary(null);  // 清空，等待AI重新处理
            existingFile.setKeywords(null);
            existingFile.setProcessStatus("PENDING");
            existingFile.setIndexed(false);  // v3.0：标记为未索引，等待新向量创建
            existingFile.setUpdateTime(LocalDateTime.now());

            fileInfoMapper.updateById(existingFile);

            log.info("新版本上传成功: fileUuid={}, version={}", existingFileUuid, existingFile.getVersion());
            return existingFile;

        } catch (Exception e) {
            log.error("上传新版本失败: {}", e.getMessage(), e);
            throw new RuntimeException("上传新版本失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileInfo> uploadMultiple(MultipartFile[] files, Long userId) {
        List<FileInfo> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(upload(file, userId));
        }
        return results;
    }

    // ===== 查询相关 =====

    @Override
    public FileInfo getByUuid(String fileUuid, Long userId) {
        FileInfo fileInfo = fileInfoMapper.selectByFileUuid(fileUuid);
        if (fileInfo == null) {
            return null;
        }
        if (!fileInfo.getUserId().equals(userId)) {
            log.warn("用户{}尝试访问不属于自己的文件{}", userId, fileUuid);
            return null;
        }
        return fileInfo;
    }

    @Override
    public FileInfo getByUuidInternal(String fileUuid) {
        return fileInfoMapper.selectByFileUuid(fileUuid);
    }

    @Override
    public List<FileInfo> getByUserId(Long userId) {
        return fileInfoMapper.selectByUserId(userId);
    }

    @Override
    public List<FileInfo> getDeletedByUserId(Long userId) {
        return fileInfoMapper.selectDeletedByUserId(userId);
    }

    @Override
    public List<FileInfo> getByCategory(Long userId, String category) {
        return fileInfoMapper.selectByCategory(userId, category);
    }

    @Override
    public List<FileInfo> search(Long userId, String keyword) {
        return fileInfoMapper.searchByKeyword(userId, keyword);
    }

    @Override
    public FileStatsDTO getStats(Long userId) {
        FileStatsDTO stats = new FileStatsDTO();

        // v3.0 fix: 只统计活跃文件（不包括已删除文件）
        List<FileInfo> allFiles = fileInfoMapper.selectByUserId(userId);

        stats.setTotalFiles((long) allFiles.size());
        stats.setActiveFiles((long) allFiles.size());

        // 已删除文件单独统计
        List<FileInfo> deletedFiles = fileInfoMapper.selectDeletedByUserId(userId);
        stats.setDeletedFiles((long) deletedFiles.size());

        Long totalSize = allFiles.stream()
                .map(FileInfo::getFileSize)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);
        stats.setTotalSize(totalSize);

        Long indexedCount = allFiles.stream()
                .filter(f -> f.getIndexed() != null && f.getIndexed())
                .count();
        stats.setIndexedFiles(indexedCount);

        Long multiVersionCount = allFiles.stream()
                .filter(f -> f.getVersion() != null && f.getVersion() > 1)
                .count();
        stats.setMultiVersionFiles(multiVersionCount);

        // 分类统计
        Map<String, Long> categoryCounts = new HashMap<>();
        for (FileInfo f : allFiles) {
            String cat = f.getCategory() != null ? f.getCategory() : "其他";
            categoryCounts.merge(cat, 1L, Long::sum);
        }
        stats.setCategoryCounts(categoryCounts);

        // 类型统计
        Map<String, Long> typeCounts = new HashMap<>();
        for (FileInfo f : allFiles) {
            String type = f.getFileType() != null ? f.getFileType() : "unknown";
            typeCounts.merge(type, 1L, Long::sum);
        }
        stats.setTypeCounts(typeCounts);

        return stats;
    }

    // ===== 版本管理 =====

    @Override
    public List<VersionHistoryItem> getVersionHistory(String fileUuid) {
        FileInfo fileInfo = fileInfoMapper.selectByFileUuid(fileUuid);
        if (fileInfo == null) {
            return Collections.emptyList();
        }

        // 构建当前版本的条目（放在第一位）
        List<VersionHistoryItem> result = new ArrayList<>();
        VersionHistoryItem currentItem = VersionHistoryParser.buildItem(
                fileInfo.getVersion(),
                fileInfo.getFileUuid(),
                fileInfo.getOriginalName(),
                fileInfo.getStoragePath(),
                fileInfo.getFileSize(),
                fileInfo.getSummary(),
                VersionHistoryParser.parseKeywords(fileInfo.getKeywords()),
                fileInfo.getCategory(),
                "当前版本",
                LocalDateTime.now()
        );
        result.add(currentItem);

        // 添加历史版本（按版本号降序排列）
        List<VersionHistoryItem> history = VersionHistoryParser.parse(fileInfo.getVersionHistory());
        history.sort((a, b) -> b.getVersion().compareTo(a.getVersion())); // 降序
        result.addAll(history);

        return result;
    }

    /**
     * 恢复历史版本（创建新版本）
     * v3.0: 不修改版本号，而是创建新版本，内容来自历史版本
     */
    @Override
    @Transactional
    public FileInfo switchVersion(String fileUuid, Integer targetVersion, Long userId) {
        FileInfo fileInfo = getByUuid(fileUuid, userId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在");
        }

        VersionHistoryItem targetItem = VersionHistoryParser.getVersion(fileInfo.getVersionHistory(), targetVersion);
        if (targetItem == null) {
            throw new RuntimeException("目标版本不存在: " + targetVersion);
        }

        // v3.0：软删除当前版本向量
        int currentVersion = fileInfo.getVersion();
        try {
            aiServiceClient.softDeleteVector(fileUuid + "_v" + currentVersion, String.valueOf(userId));
            log.info("当前版本向量软删除: fileUuid={}, version={}", fileUuid, currentVersion);
        } catch (Exception e) {
            log.warn("当前版本向量软删除失败: {}", e.getMessage());
        }

        // v3.0：尝试恢复目标版本向量
        try {
            aiServiceClient.restoreVector(fileUuid + "_v" + targetVersion, String.valueOf(userId));
            log.info("目标版本向量恢复: fileUuid={}, version={}", fileUuid, targetVersion);
        } catch (Exception e) {
            log.warn("目标版本向量恢复失败（可能不存在）: {}", e.getMessage());
        }

        // v3.0: 先保存当前版本到历史（因为要创建新版本）
        VersionHistoryItem currentHistory = VersionHistoryParser.buildItem(
                fileInfo.getVersion(),
                fileInfo.getFileUuid(),
                fileInfo.getOriginalName(),
                fileInfo.getStoragePath(),
                fileInfo.getFileSize(),
                fileInfo.getSummary(),
                VersionHistoryParser.parseKeywords(fileInfo.getKeywords()),
                fileInfo.getCategory(),
                "版本" + fileInfo.getVersion() + " (恢复前)",
                fileInfo.getCreateTime()
        );

        String updatedHistory = VersionHistoryParser.addVersion(fileInfo.getVersionHistory(), currentHistory);

        // 创建新版本：内容来自历史版本，但版本号递增
        int newVersion = fileInfo.getVersion() + 1;

        // 使用历史版本的物理文件路径（不复制，直接引用）
        // 注意：这样切换版本后，物理文件还是历史版本的，但版本号是新版本
        fileInfo.setVersion(newVersion);
        fileInfo.setOriginalName(targetItem.getOriginalName());
        fileInfo.setStoragePath(targetItem.getStoragePath());
        fileInfo.setFileSize(targetItem.getFileSize());
        fileInfo.setSummary(targetItem.getSummary());
        fileInfo.setKeywords(VersionHistoryParser.keywordsToJson(targetItem.getKeywords()));
        fileInfo.setCategory(targetItem.getCategory());
        fileInfo.setVersionHistory(updatedHistory);
        fileInfo.setUpdateTime(LocalDateTime.now());
        fileInfo.setProcessStatus("COMPLETED"); // 已有AI结果，无需重新处理
        fileInfo.setIndexed(true); // v3.0：向量已恢复，标记为已索引

        fileInfoMapper.updateById(fileInfo);

        log.info("版本恢复成功: fileUuid={}, 从v{}恢复为新版本v{}", fileUuid, targetVersion, newVersion);
        return fileInfo;
    }

    @Override
    public byte[] downloadVersion(String fileUuid, Integer version, Long userId) {
        FileInfo fileInfo = getByUuid(fileUuid, userId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在或无权限");
        }

        String storagePath;
        if (version.equals(fileInfo.getVersion())) {
            storagePath = fileInfo.getStoragePath();
        } else {
            VersionHistoryItem historyItem = VersionHistoryParser.getVersion(fileInfo.getVersionHistory(), version);
            if (historyItem == null) {
                throw new RuntimeException("历史版本不存在: " + version);
            }
            storagePath = historyItem.getStoragePath();
        }

        return downloadFromMinio(storagePath);
    }

    // ===== 删除恢复 =====

    @Override
    @Transactional
    public void softDelete(String fileUuid, Long userId) {
        int result = fileInfoMapper.softDelete(fileUuid, userId);
        if (result == 0) {
            throw new RuntimeException("文件不存在或无权限");
        }

        // v3.0：调用向量软删除（标记deleted=true，搜索过滤）
        try {
            aiServiceClient.softDeleteVector(fileUuid, String.valueOf(userId));
            log.info("向量软删除成功: fileUuid={}", fileUuid);
        } catch (Exception e) {
            log.warn("向量软删除调用失败（文件已标记删除）: {}", e.getMessage());
        }

        // 标记本地向量状态
        fileInfoMapper.markVectorDeleted(fileUuid);

        log.info("文件软删除成功: fileUuid={}", fileUuid);
    }

    @Override
    @Transactional
    public void restore(String fileUuid, Long userId) {
        int result = fileInfoMapper.restore(fileUuid, userId);
        if (result == 0) {
            throw new RuntimeException("文件不存在或无权限");
        }

        // v3.0：调用向量恢复（标记deleted=false，恢复可检索）
        try {
            aiServiceClient.restoreVector(fileUuid, String.valueOf(userId));
            log.info("向量恢复成功: fileUuid={}", fileUuid);
        } catch (Exception e) {
            log.warn("向量恢复调用失败（文件已恢复）: {}", e.getMessage());
        }

        log.info("文件恢复成功: fileUuid={}", fileUuid);
    }

    @Override
    @Transactional
    public void permanentDelete(String fileUuid, Long userId) {
        FileInfo fileInfo = getByUuid(fileUuid, userId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在或无权限");
        }

        // 删除MinIO物理文件
        try {
            String objectName = extractObjectName(fileInfo.getStoragePath());
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.warn("MinIO删除失败: {}", e.getMessage());
        }

        // 删除历史版本文件
        List<VersionHistoryItem> history = getVersionHistory(fileUuid);
        for (VersionHistoryItem item : history) {
            try {
                String historyObjectName = extractObjectName(item.getStoragePath());
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioConfig.getBucket())
                                .object(historyObjectName)
                                .build()
                );
            } catch (Exception e) {
                log.warn("历史版本MinIO删除失败: {}", e.getMessage());
            }
        }

        // 物理删除数据库记录
        fileInfoMapper.deleteById(fileInfo.getId());

        log.info("文件永久删除成功: fileUuid={}", fileUuid);
    }

    // ===== 批量操作 =====

    @Override
    @Transactional
    public void batchDelete(List<String> fileUuids, Long userId) {
        for (String fileUuid : fileUuids) {
            softDelete(fileUuid, userId);
        }
    }

    @Override
    @Transactional
    public void batchClassify(List<String> fileUuids, String category, Long userId) {
        for (String fileUuid : fileUuids) {
            fileInfoMapper.updateCategory(fileUuid, userId, category);
        }
    }

    // ===== 修改相关 =====

    @Override
    public void updateCategory(String fileUuid, String category, Long userId) {
        int result = fileInfoMapper.updateCategory(fileUuid, userId, category);
        if (result == 0) {
            throw new RuntimeException("文件不存在或无权限");
        }
    }

    @Override
    public void updateAiResult(String fileUuid, String summary, String keywords, String category) {
        fileInfoMapper.updateAiResult(fileUuid, summary, keywords, category);
        log.info("AI结果更新成功: fileUuid={}", fileUuid);
    }

    @Override
    public void updateProcessStatus(String fileUuid, String status) {
        fileInfoMapper.updateProcessStatus(fileUuid, status);
    }

    // ===== 下载相关 =====

    @Override
    public byte[] download(String fileUuid, Long userId) {
        FileInfo fileInfo = getByUuid(fileUuid, userId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在或无权限");
        }
        // 使用storagePath中实际存储的object name
        return downloadFromMinio(extractObjectName(fileInfo.getStoragePath()));
    }

    @Override
    public String getPreviewUrl(String fileUuid, Long userId) {
        FileInfo fileInfo = getByUuid(fileUuid, userId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在或无权限");
        }

        try {
            // 使用storagePath中实际存储的object name
            String objectName = extractObjectName(fileInfo.getStoragePath());
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("获取预览URL失败: " + e.getMessage());
        }
    }

    // ===== 分享相关 =====

    @Override
    @Transactional
    public void shareToUsers(String fileUuid, List<Long> shareToIds, String permission, Integer expireDays, Long userId) {
        FileInfo fileInfo = getByUuid(fileUuid, userId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在或无权限");
        }

        LocalDateTime expireTime = expireDays != null ? LocalDateTime.now().plusDays(expireDays) : null;

        for (Long shareToId : shareToIds) {
            FileShare share = new FileShare();
            share.setFileUuid(fileUuid);
            share.setOwnerId(userId);
            share.setShareToId(shareToId);
            share.setPermission(permission);
            share.setExpireTime(expireTime);
            share.setStatus("ACTIVE");
            share.setCreateTime(LocalDateTime.now());

            fileShareMapper.insert(share);
        }

        log.info("分享成功: fileUuid={}, shareToIds={}", fileUuid, shareToIds);
    }

    @Override
    @Transactional
    public String createPublicShare(String fileUuid, Integer expireDays, String password, Integer downloadLimit, Long userId) {
        FileInfo fileInfo = getByUuid(fileUuid, userId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在或无权限");
        }

        String shareCode = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        PublicShare publicShare = new PublicShare();
        publicShare.setShareCode(shareCode);
        publicShare.setFileUuid(fileUuid);
        publicShare.setOwnerId(userId);
        publicShare.setPassword(password);
        publicShare.setDownloadLimit(downloadLimit != null ? downloadLimit : -1);
        publicShare.setDownloadCount(0);
        publicShare.setExpireTime(expireDays != null ? LocalDateTime.now().plusDays(expireDays) : null);
        publicShare.setStatus("ACTIVE");
        publicShare.setCreateTime(LocalDateTime.now());

        publicShareMapper.insert(publicShare);

        log.info("公开分享创建成功: shareCode={}", shareCode);
        return shareCode;
    }

    @Override
    public FileInfo getPublicSharedFile(String shareCode, String password) {
        PublicShare publicShare = publicShareMapper.selectByShareCode(shareCode);
        if (publicShare == null) {
            throw new RuntimeException("分享不存在或已过期");
        }

        if (publicShare.getPassword() != null && !publicShare.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }

        return fileInfoMapper.selectByFileUuid(publicShare.getFileUuid());
    }

    @Override
    public List<FileInfo> getSharedToMe(Long userId) {
        List<FileShare> shares = fileShareMapper.selectSharedToMe(userId);
        List<FileInfo> files = new ArrayList<>();
        for (FileShare share : shares) {
            FileInfo fileInfo = fileInfoMapper.selectByFileUuid(share.getFileUuid());
            if (fileInfo != null) {
                files.add(fileInfo);
            }
        }
        return files;
    }

    // ===== 相似检测 =====

    @Override
    public SimilarityResultDTO checkSimilarity(String fileName, Long userId) {
        // v3.0：使用统一的SimilarityService
        return similarityService.checkSimilarity(fileName, userId);
    }

    // ===== 辅助方法 =====

    private String generateStorageFileName(String uuid, String originalName) {
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        return uuid + extension;
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "unknown";
        String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase() : "";
        return switch (extension) {
            case "pdf" -> "pdf";
            case "doc", "docx" -> "word";
            case "xls", "xlsx" -> "excel";
            case "ppt", "pptx" -> "ppt";
            case "txt" -> "text";
            case "md" -> "markdown";
            default -> "other";
        };
    }

    private byte[] downloadFromMinio(String storagePath) {
        try {
            // v3.0 fix: 使用配置的bucket名称，而不是从storagePath解析
            String objectName = storagePath.contains("/")
                ? storagePath.substring(storagePath.indexOf("/") + 1)
                : storagePath;

            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .build()
            );

            return response.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    private String extractObjectName(String storagePath) {
        if (storagePath == null || !storagePath.contains("/")) {
            return storagePath;
        }
        return storagePath.substring(storagePath.indexOf("/") + 1);
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        s1 = s1.toLowerCase().trim();
        s2 = s2.toLowerCase().trim();

        if (s1.equals(s2)) return 1.0;

        // Levenshtein距离简化版
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

    // ===== 重新处理 =====

    @Override
    public void reprocessFile(String fileUuid, Long userId) {
        FileInfo fileInfo = getByUuid(fileUuid, userId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在或无权限");
        }

        // 重置处理状态
        fileInfo.setProcessStatus("PENDING");
        fileInfo.setIndexed(false);
        fileInfoMapper.updateById(fileInfo);

        // 触发处理链
        fileProcessService.processFile(fileUuid, userId);
        log.info("已触发重新处理: fileUuid={}", fileUuid);
    }

    @Override
    public int batchReprocessUnindexed(Long userId) {
        List<FileInfo> files = fileInfoMapper.selectUnindexedByUserId(userId);
        int count = 0;
        for (FileInfo file : files) {
            // 重置处理状态
            file.setProcessStatus("PENDING");
            file.setIndexed(false);
            fileInfoMapper.updateById(file);

            // 触发处理链
            fileProcessService.processFile(file.getFileUuid(), userId);
            count++;
        }
        log.info("批量触发重新处理: userId={}, count={}", userId, count);
        return count;
    }
}