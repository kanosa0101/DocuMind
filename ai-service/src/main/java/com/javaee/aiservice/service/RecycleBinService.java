package com.javaee.aiservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.javaee.aiservice.entity.RecycleBin;
import com.javaee.aiservice.mapper.RecycleBinMapper;
import com.javaee.aiservice.vo.RecycleBinVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 回收站服务
 * 功能说明：管理已删除的文件，支持在有效期内恢复
 * 实现方式：使用数据库持久化存储，支持跨实例共享和重启后数据保留
 */
@Service
public class RecycleBinService {

    private static final Logger log = LoggerFactory.getLogger(RecycleBinService.class);

    @Autowired
    private RecycleBinMapper recycleBinMapper;

    @Autowired
    private MinIOService minIOService;

    @Value("${minio.bucket:documents}")
    private String defaultBucket;

    @Value("${minio.recycle.expiry-days:7}")
    private int recycleExpiryDays;

    /**
     * 将文件移动到回收站
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param userId 删除用户ID
     * @return 回收站记录ID
     */
    @Transactional
    public String moveToRecycleBin(String bucketName, String objectName, Long userId) {
        log.info("将文件移动到回收站: bucket={}, object={}, userId={}", bucketName, objectName, userId);

        try {
            String recycleId = UUID.randomUUID().toString();
            LocalDateTime deleteTime = LocalDateTime.now();
            LocalDateTime expiryTime = deleteTime.plusDays(recycleExpiryDays);

            // 获取文件信息
            Long fileSize = 0L;
            String fileType = null;
            String originalFileName = objectName;
            String fileId = null;

            // 从 objectName 提取 fileId（objectName = fileId + 扩展名）
            // 例如：abc123.pdf -> fileId = abc123
            if (objectName != null && objectName.contains(".")) {
                int lastDotIndex = objectName.lastIndexOf(".");
                fileId = objectName.substring(0, lastDotIndex);
                originalFileName = objectName; // 保留完整文件名作为显示名称
            } else if (objectName != null) {
                // 没有扩展名，objectName 就是 fileId
                fileId = objectName;
            }

            try {
                // 尝试从MinIO获取文件元数据
                var stat = minIOService.getFileMetadata(bucketName, objectName);
                if (stat != null) {
                    fileSize = stat.size();
                    fileType = stat.contentType();
                }
            } catch (Exception e) {
                log.warn("无法获取文件元数据: {}", e.getMessage());
            }

            RecycleBin recycleBin = new RecycleBin();
            recycleBin.setRecycleId(recycleId);
            recycleBin.setFileId(fileId);
            recycleBin.setBucketName(bucketName);
            recycleBin.setObjectName(objectName);
            recycleBin.setOriginalFileName(originalFileName);
            recycleBin.setFileSize(fileSize);
            recycleBin.setFileType(fileType);
            recycleBin.setUserId(userId);
            recycleBin.setDeleteTime(deleteTime);
            recycleBin.setExpiryTime(expiryTime);
            recycleBin.setStatus("DELETED");
            recycleBin.setCreateTime(deleteTime);
            recycleBin.setUpdateTime(deleteTime);

            recycleBinMapper.insert(recycleBin);

            log.info("文件已移动到回收站: recycleId={}, expiryTime={}", recycleId, expiryTime);
            return recycleId;

        } catch (Exception e) {
            log.error("移动文件到回收站失败", e);
            throw new RuntimeException("移动文件到回收站失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从回收站恢复文件
     *
     * @param recycleId 回收站记录ID
     * @param newObjectName 新对象名称（可选）
     * @return 恢复后的对象名称
     */
    @Transactional
    public String restoreFromRecycleBin(String recycleId, String newObjectName) {
        log.info("从回收站恢复文件: recycleId={}", recycleId);

        try {
            RecycleBin recycleBin = recycleBinMapper.selectByRecycleId(recycleId);
            if (recycleBin == null) {
                throw new IllegalArgumentException("回收站记录不存在: " + recycleId);
            }

            if (!"DELETED".equals(recycleBin.getStatus())) {
                throw new IllegalStateException("文件已被恢复或永久删除");
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(recycleBin.getExpiryTime())) {
                throw new IllegalStateException("文件已超过回收站保留期，无法恢复");
            }

            String targetObjectName = newObjectName != null ? newObjectName : recycleBin.getObjectName();

            log.info("从回收站恢复文件: original={}, target={}",
                recycleBin.getObjectName(), targetObjectName);

            // 更新状态为已恢复
            recycleBinMapper.updateStatus(recycleId, "RESTORED");

            return targetObjectName;

        } catch (Exception e) {
            log.error("从回收站恢复文件失败", e);
            throw new RuntimeException("从回收站恢复文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取回收站文件列表
     *
     * @param bucketName 存储桶名称（可选）
     * @return 回收站文件列表
     */
    public RecycleBinVO listRecycleBin(String bucketName) {
        log.info("获取回收站文件列表: bucket={}", bucketName);

        try {
            List<RecycleBin> recycleBins;

            if (bucketName != null && !bucketName.isEmpty()) {
                recycleBins = recycleBinMapper.selectByBucketName(bucketName);
            } else {
                // 查询所有未过期的回收站文件
                LambdaQueryWrapper<RecycleBin> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(RecycleBin::getStatus, "DELETED")
                       .gt(RecycleBin::getExpiryTime, LocalDateTime.now())
                       .orderByDesc(RecycleBin::getDeleteTime);
                recycleBins = recycleBinMapper.selectList(wrapper);
            }

            List<RecycleBinVO.RecycleFile> files = recycleBins.stream()
                .map(this::convertToRecycleFile)
                .collect(Collectors.toList());

            RecycleBinVO vo = new RecycleBinVO();
            vo.setTotalCount(files.size());
            vo.setFiles(files);

            log.info("回收站文件列表: count={}", files.size());
            return vo;

        } catch (Exception e) {
            log.error("获取回收站文件列表失败", e);
            throw new RuntimeException("获取回收站文件列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据用户ID获取回收站文件列表
     *
     * @param userId 用户ID
     * @return 回收站文件列表
     */
    public RecycleBinVO listRecycleBinByUserId(Long userId) {
        log.info("获取用户回收站文件列表: userId={}", userId);

        try {
            List<RecycleBin> recycleBins = recycleBinMapper.selectByUserId(userId);

            List<RecycleBinVO.RecycleFile> files = recycleBins.stream()
                .map(this::convertToRecycleFile)
                .collect(Collectors.toList());

            RecycleBinVO vo = new RecycleBinVO();
            vo.setTotalCount(files.size());
            vo.setFiles(files);

            log.info("用户回收站文件列表: userId={}, count={}", userId, files.size());
            return vo;

        } catch (Exception e) {
            log.error("获取用户回收站文件列表失败", e);
            throw new RuntimeException("获取用户回收站文件列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 永久删除回收站中的文件
     *
     * @param recycleId 回收站记录ID
     */
    @Transactional
    public void permanentDelete(String recycleId) {
        log.info("永久删除回收站中的文件: recycleId={}", recycleId);

        try {
            RecycleBin recycleBin = recycleBinMapper.selectByRecycleId(recycleId);
            if (recycleBin == null) {
                throw new IllegalArgumentException("回收站记录不存在: " + recycleId);
            }

            // 从MinIO删除物理文件
            try {
                minIOService.deleteFile(recycleBin.getBucketName(), recycleBin.getObjectName());
                log.info("MinIO物理文件已删除: bucket={}, object={}",
                    recycleBin.getBucketName(), recycleBin.getObjectName());
            } catch (Exception e) {
                log.warn("删除MinIO物理文件失败: {}", e.getMessage());
            }

            // 更新状态为永久删除
            recycleBinMapper.updateStatus(recycleId, "PERMANENT_DELETED");

            log.info("文件已永久删除: recycleId={}", recycleId);

        } catch (Exception e) {
            log.error("永久删除文件失败", e);
            throw new RuntimeException("永久删除文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 定时清理过期的回收站文件
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredFiles() {
        log.info("开始清理过期的回收站文件");

        try {
            List<RecycleBin> expiredFiles = recycleBinMapper.selectExpired();
            int removedCount = 0;

            for (RecycleBin recycleBin : expiredFiles) {
                try {
                    // 从MinIO删除物理文件
                    minIOService.deleteFile(recycleBin.getBucketName(), recycleBin.getObjectName());
                    removedCount++;
                } catch (Exception e) {
                    log.warn("删除过期文件失败: recycleId={}, error={}",
                        recycleBin.getRecycleId(), e.getMessage());
                }
            }

            // 批量删除过期记录
            if (!expiredFiles.isEmpty()) {
                recycleBinMapper.deleteExpired();
            }

            log.info("清理完成，删除了 {} 个过期文件", removedCount);

        } catch (Exception e) {
            log.error("清理过期文件失败", e);
        }
    }

    /**
     * 将RecycleBin实体转换为RecycleFile VO
     */
    private RecycleBinVO.RecycleFile convertToRecycleFile(RecycleBin recycleBin) {
        RecycleBinVO.RecycleFile file = new RecycleBinVO.RecycleFile();
        file.setRecycleId(recycleBin.getRecycleId());
        file.setBucketName(recycleBin.getBucketName());
        file.setOriginalObjectName(recycleBin.getObjectName());
        file.setDeleteTime(recycleBin.getDeleteTime() != null ?
            recycleBin.getDeleteTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L);
        file.setExpiryTime(recycleBin.getExpiryTime() != null ?
            recycleBin.getExpiryTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L);
        file.setFileSize(recycleBin.getFileSize() != null ? recycleBin.getFileSize() : 0L);
        file.setDeleter(String.valueOf(recycleBin.getUserId()));
        return file;
    }
}