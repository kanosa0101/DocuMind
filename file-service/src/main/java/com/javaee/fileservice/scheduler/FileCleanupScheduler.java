package com.javaee.fileservice.scheduler;

import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.mapper.FileInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件清理定时任务
 * 每天凌晨2点清理30天前软删除的文件
 */
@Component
public class FileCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupScheduler.class);

    private final FileInfoMapper fileInfoMapper;

    public FileCleanupScheduler(FileInfoMapper fileInfoMapper) {
        this.fileInfoMapper = fileInfoMapper;
    }

    /**
     * 每天02:00执行清理任务
     * 删除deleteTime超过30天的文件记录
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredFiles() {
        log.info("开始执行文件清理任务...");

        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        // 查询过期文件（deleteTime > 30天）
        List<FileInfo> expiredFiles = fileInfoMapper.findDeletedBefore(threshold);

        if (expiredFiles.isEmpty()) {
            log.info("无过期文件需要清理");
            return;
        }

        // 真删除：从数据库移除
        for (FileInfo file : expiredFiles) {
            fileInfoMapper.deleteById(file.getId());
            log.info("已清理过期文件: {} (deleteTime: {})",
                     file.getOriginalName(), file.getDeleteTime());
        }

        log.info("清理完成，共清理 {} 个过期文件", expiredFiles.size());
    }
}